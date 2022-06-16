package server.route

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import server.ifLogin
import utils.PortalAccessManagement
import utils.STATIC_FILES

fun Routing.clusterRoute(){
    get("/portal/{portal}") {
        ifLogin {
            if (call.parameters.contains("portal")) {
                val portal = call.parameters["portal"]!!
                if(PortalAccessManagement.canAccess(it, portal)){
                    call.respondFile(STATIC_FILES.findFile("Portal.html"))
                }else{
                    call.respondRedirect("/")
                }
            } else {
                call.respondRedirect("/")
            }
        }
    }
}