package server.route

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import server.handleDataPost
import utils.STATIC_FILES

fun Routing.commonRoute(){

    get("/utils.js"){
        call.respondFile(STATIC_FILES.findFile("utils.js"))
    }
    get("/logo.svg"){
        call.respondFile(STATIC_FILES.findFile("logo.svg"))
    }

    handleDataPost("/login"){

    }
}