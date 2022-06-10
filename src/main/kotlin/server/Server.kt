package server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

object Server {
    suspend fun start(){
        embeddedServer(Netty, port = 5000, host = "0.0.0.0") {
            routing {
                get("/"){
                    call.respondText("On")
                }
            }
        }.start(wait = true)
    }
}