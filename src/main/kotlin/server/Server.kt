package server

import PORT
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.html.currentTimeMillis
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import server.route.clusterRoute
import server.route.hostRoute
import server.route.userRoute
import server.trace.Traceable
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

object Server {
    suspend fun start() {
        println("Starting server at 0.0.0.0:${PORT}")
        embeddedServer(Netty, port = PORT, host = "0.0.0.0") {
            routing {
                userRoute()
                clusterRoute()
                hostRoute()
            }
        }.start(wait = true)
    }
}


val ServerJson = Json {
    this.ignoreUnknownKeys = true
    this.isLenient = true
    this.encodeDefaults = true
}

data class ServerResponse<T>(
    val success: Boolean,
    val errorMessage: String = "",

    val isTracingTask: Boolean,

    val data: T
)

data class ServerRequest<T>(
    val data: T,
    val identifier: String,
    val requestId: String,
    val requestTime: Long
)

/**
 * Use to indicate this error is triggered by user's input
 * The server response won't include stacktrace for UserInputError
 */
class UserInputError(message: String): Exception(message)


@kotlin.jvm.Throws(UserInputError::class)
fun userInputError(message: Any):Nothing = throw UserInputError(message.toString())

private suspend fun <T> ApplicationCall.respond(response: ServerResponse<T>) {
    this.respond(ServerJson.encodeToString(response))
}

/**
 * Respond to front end that some Exception/Error occurred during the process
 * This could cause by invalid arguments, or other service issue (RestAPI Timeout)
 */
suspend fun ApplicationCall.respondThrowable(e: Throwable) {
    val errorMessage = if (e is UserInputError) {
        e.message!!
    } else {
        e.message + "</br>" + e.stackTrace.joinToString("</br>")
    }

    this.respond(
        ServerResponse(
            success = false,
            errorMessage = errorMessage,
            isTracingTask = false,
            data = null
        )
    )
}

/**
 * Respond to front end that the process has COMPLETE and OK
 */
suspend fun ApplicationCall.respondOK() {
    this.respond(
        ServerResponse(
            success = true,
            errorMessage = "",
            isTracingTask = false,
            data = "OK"
        )
    )
}

/**
 * Respond to front end that the process has COMPLETE and OK
 * plus the data front end need
 */
suspend fun <T : Any> ApplicationCall.respondOK(data: T) {
    this.respond(
        ServerResponse(
            success = true,
            errorMessage = "",
            isTracingTask = false,
            data = data
        )
    )
}


suspend fun <T : Any> ApplicationCall.respondTraceable(traceable: Traceable<T>) {
    when (traceable.state) {
        Traceable.State.SCHEDULING ->
            this.respond(ServerResponse(
                success = true,
                errorMessage = "",
                isTracingTask = true,
                data = traceable.toTracingData()
            ))
        Traceable.State.COMPUTED ->
            this.respondOK(traceable.getResult())
        Traceable.State.THROWN ->
            this.respondThrowable(traceable.getFailureReason())
    }
}




fun Routing.handleDataPost(path: String, receiver: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit) {
    post(path) {
        try {
            this.receiver()
        } catch (e: Throwable) {
            call.respondThrowable(e)
        }
    }
}
suspend inline fun <reified T : Any> ApplicationCall.readDataRequest(): T {
    val text = String(receiveStream().readBytes(), Charsets.UTF_8)

    val req: ServerRequest<T> = try {
         ServerJson.decodeFromString(text)
    }catch (e: SerializationException){
        userInputError("bad input $text")
    }

    return RequestShield(req)
}



object RequestShield{
    private const val recordSize = 100
    private val recentRequests = Array<String?>(recordSize) { null }
    private val set = hashSetOf<String>()
    private var indexer = 0
    private val lock = Mutex()
    private const val timeDelay = 12000


    suspend operator fun <T : Any> invoke(input: ServerRequest<T>):T {
        val current = currentTimeMillis()
        val request = input.requestTime
        val visitor = input.requestId

        if (visitor.length > 50) {
            userInputError("bad request")
        }
        val visitorData = visitor.split("-")
        if (visitorData.size != 5) {
            userInputError("bad request")
        }
        if (abs(current - request) > timeDelay) {
            userInputError("request expired")
        }
        lock.withLock {
            if (set.contains(visitor)) {
                userInputError("repeated request")
            }
            val index = indexer % recordSize

            val toRemove = recentRequests[index]
            if(toRemove != null){
                set.remove(toRemove)
            }
            set.add(visitor)
            recentRequests[index] = visitor
            indexer += 1

            if(indexer > recordSize){
                indexer-= recordSize
            }
        }

        return input.data
    }
}