package ssh

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.session.ClientSession
import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import java.util.logging.Level
import java.util.logging.Logger

class SSHCommandExecuteException(override val message: String):Exception(message)


class HCDSshClient(
    val name:String,
    private val session: ClientSession
) {
    private val logger = PrintStream(object: OutputStream(){
        private var mem = StringBuffer()
        override fun write(b: Int) {
            val bytes = ByteArray(1)
            bytes[0] = (b and 0xff).toByte()
            mem.append(String(bytes))
            if (mem.endsWith ("\n")) {
                print("[ssh-$name] $mem")
                mem.setLength(0)
            }
        }
    })

    companion object{
        init {
            Logger.getLogger("io.netty").level = Level.OFF
            Logger.getGlobal().level = Level.SEVERE
            Logger.getLogger("org.apache.ftpserver.listener.nio.FtpLoggingFilter").level = Level.SEVERE
            Logger.getLogger("io.netty.handler.logging.LoggingHandler").level = Level.SEVERE
            org.slf4j.LoggerFactory.getLogger("io.netty")
            // stop fucking loggers (By Smart Him188)
        }

        operator fun invoke(
            name: String,
            address: String,
            username:String = "hcd",
            password:String = "hcd"
        ):HCDSshClient{
            val client = SshClient.setUpDefaultClient()
            client.start()
            client.connect(username, address, 22)
                .verify()
                .session.run {
                    addPasswordIdentity(password)
                    auth().verify()
                    return HCDSshClient(name, this)
                }
        }
    }

    fun execute(cmd: String) {
        logger.println("> Executing:  $cmd")
        kotlin.runCatching { session.executeRemoteCommand(cmd, logger, logger, Charset.defaultCharset()) }
            .onFailure {
                logger.println(it.message)
                throw SSHCommandExecuteException("Failed to execute $cmd").apply {
                    addSuppressed(it)
                }
            }
    }

}
