package operation.pipeline

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.html.currentTimeMillis
import operation.request.Pipeline
import operation.request.Requester
import operation.request.Response
import utils.LogColor
import utils.LogColorOutputType
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.log

class AsyncLogger(val output: PrintStream = System.out, val colorCodeType: LogColorOutputType) : Pipeline {
    /** Thread safe */
    private val logs = StringBuffer()

    companion object {
        val printLock = Mutex()
        /* omit suspend */
        private val inProgressLoggers = Collections.synchronizedList(mutableListOf<AsyncLogger>())

        init {
            Runtime.getRuntime().addShutdownHook(thread(start = false) {
                inProgressLoggers.forEach {
                    it.output.println()
                    it.output.println(it.logs.toString())
                    it.output.println()
                }
            })
        }
    }

    init {
        inProgressLoggers.add(this)
    }

    fun log(color: LogColor, message: Any){
        val strDateFormat = "MM-dd HH:mm:ss"
        val sdf = SimpleDateFormat(strDateFormat)
        output.println(buildString{
            append(color.toString(this@AsyncLogger.colorCodeType))
            append(" ")
            append(sdf.format(currentTimeMillis()))
            append(" :")
            append(message)
            append(LogColor.RESET.toString(colorCodeType))
        })
    }

    override suspend fun beforeRequest(request: Requester, data: Any) {
        log(LogColor.TEAL,request.method.toString() + " " + request.buildURL().buildString())
        log(LogColor.TEAL,request.data)

    }

    override suspend fun afterResponse(request: Requester, response: Response) {
        val color = if(response.statusCode < 400){
            LogColor.GREEN
        }else{
            LogColor.RED
        }

        (response.statusCode)
        logs.append(response.body)

        printLock.withLock {
            output.println(" ")
            output.println(logs.toString())
            output.println(" ")
        }

        inProgressLoggers.remove(this)
    }

}