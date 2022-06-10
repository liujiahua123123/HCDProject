package server

import PORT
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

object Server {
    suspend fun start(){
        println("Starting server at 0.0.0.0:${PORT}" )
        embeddedServer(Netty, port = PORT, host = "0.0.0.0") {
            routing {
                get("/"){
                    call.respondText("On")
                }
            }
        }.start(wait = true)
    }
}