package server

import PORT
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import server.route.clusterRoute
import server.route.hostRoute
import server.route.userRoute
import server.trace.Traceable

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


private suspend fun <T> ApplicationCall.respond(response: ServerResponse<T>) {
    this.respond(ServerJson.encodeToString(response))
}

/**
 * Respond to front end that some Exception/Error occurred during the process
 * This could cause by invalid arguments, or other service issue (RestAPI Timeout)
 */
suspend fun ApplicationCall.respondThrowable(e: Throwable) {
    val errorMessage = if (e is Error) {
        e.message ?: e.stackTrace.joinToString("</br>")
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
suspend fun ApplicationCall.respondOK(e: Throwable) {
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
