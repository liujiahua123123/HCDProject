package server.route

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import operation.httpOperationScope
import operation.volumeaccessgroup.CreateVolumeAccessGroupOperation
import operation.volumeaccessgroup.CreateVolumeAccessGroupReq
import server.handleDataPost
import server.ifFromPortalPage
import server.readDataRequest
import server.respondOK

fun Routing.volumeAccessGroupRouting() {
    handleDataPost("/volume-access-group/create"){
        ifFromPortalPage { user, portal ->
            val req = call.readDataRequest<CreateVolumeAccessGroupReq>()
            httpOperationScope(portal){
                create<CreateVolumeAccessGroupOperation>().invoke(req)
                call.respondOK()
            }
        }
    }
}