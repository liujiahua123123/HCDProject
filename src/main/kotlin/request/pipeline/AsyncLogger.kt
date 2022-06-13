package request.pipeline

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.html.currentTimeMillis
import operation.request.Pipeline
import operation.request.Requester
import operation.request.Response
import utils.LogColor
import utils.LogColorOutputType
import java.awt.Color
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.log

/**
 * Async Logger provide the ability to log every request continually in the output stream (impl by cache)
 * It also provides the ability to warn Throwable and log Throwable
 * It guarantees cache will be flush to the print stream before runtime shutdown
 */
class AsyncLogger(val loggerName: String, val output: PrintStream = System.out, private val colorCodeType: LogColorOutputType = LogColorOutputType.LinuxCode
) : Pipeline {
    /** Thread safe */
    private val logs = StringBuffer()

    override val priority: Int
    get() = Int.MAX_VALUE

    companion object {
        val printLock = Mutex()
        /* omit suspend */
        private val inProgressLoggers = Collections.synchronizedList(mutableListOf<AsyncLogger>())

        init {
            /**
             * save log from force runtime shut-down
             */
            Runtime.getRuntime().addShutdownHook(thread(start = false) {
                inProgressLoggers.forEach {
                    it.log(LogColor.RED, "This request is terminated because of runtime shutdown")
                    it.output.println(" ")
                    it.output.println(it.logs.toString())
                    it.output.println(" ")
                }
            })
        }
    }

    init {
        inProgressLoggers.add(this)
    }

    private suspend fun flush(){
        printLock.withLock {
            output.println(" ")
            output.println(logs.toString())
            output.println(" ")
        }
        logs.delete(0,logs.length)
        inProgressLoggers.remove(this)
    }

    private fun log(color: LogColor, message: Any){
        val strDateFormat = "MM-dd HH:mm:ss"
        val sdf = SimpleDateFormat(strDateFormat)
        logs.append(buildString{
            append(color.toString(this@AsyncLogger.colorCodeType))
            append("[")
            append(loggerName)
            append(" ")
            append(sdf.format(currentTimeMillis()))
            append("] ")
            append(message)
            append(LogColor.RESET.toString(colorCodeType))
            append("\n")
        })
    }

    override suspend fun beforeRequest(request: Requester, data: Any) {
        log(LogColor.TEAL, "${request.method} ${request.buildURL().buildString()}")
        log(LogColor.TEAL,request.data)
    }

    override suspend fun afterResponse(request: Requester, response: Response) {
        val color = if(response.statusCode < 400){
            LogColor.GREEN
        }else{
            LogColor.RED
        }

        log(color,response.statusCode)
        log(color,response.body)
        flush()
    }


    override suspend fun onThrowable(request: Requester, throwable: Throwable) {
        log(LogColor.RED, "Exception ${throwable.javaClass.simpleName} happened, saving logs and stacktrace")
        flush()
    }
}