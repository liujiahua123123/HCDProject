package server.route

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import operation.httpOperationScope
import operation.volumeaccessgroup.*
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
    handleDataPost("/volume-access-group/add-initiator"){
        ifFromPortalPage { _, portal ->
            val req = call.readDataRequest<UpdateVolumeAccessGroupRequest>()
            httpOperationScope(portal){
                create<AddVolumeAccessGroupInitiatorOperation>().invoke(AddVolumeAccessGroupInitiatorReq(
                    volumeAccessGroupId = req.volumeAccessGroupId,
                    clusterId = req.clusterId,
                    initiatorId = req.id
                ))
                call.respondOK()
            }
        }
    }
    handleDataPost("/volume-access-group/add-volume"){
        ifFromPortalPage { _, portal ->
            val req = call.readDataRequest<UpdateVolumeAccessGroupRequest>()
            httpOperationScope(portal){
                create<AddVolumeAccessGroupVolumeOperation>().invoke(AddVolumeAccessGroupVolumeReq(
                    volumeAccessGroupId = req.volumeAccessGroupId,
                    clusterId = req.clusterId,
                    volumeId = req.id
                ))
                call.respondOK()
            }
        }
    }
    handleDataPost("/volume-access-group/remove-volume"){
        ifFromPortalPage { _, portal ->
            val req = call.readDataRequest<UpdateVolumeAccessGroupRequest>()
            httpOperationScope(portal){
                create<RemoveVolumeAccessGroupVolumeOperation>().invoke(RemoveVolumeAccessGroupVolumeReq(
                    volumeAccessGroupId = req.volumeAccessGroupId,
                    clusterId = req.clusterId,
                    volumeId = req.id
                ))
                call.respondOK()
            }
        }
    }
    handleDataPost("/volume-access-group/remove-initiator"){
        ifFromPortalPage{ _, portal ->
            val req = call.readDataRequest<UpdateVolumeAccessGroupRequest>()
            httpOperationScope(portal){
                create<RemoveVolumeAccessGroupInitiatorOperation>().invoke(RemoveVolumeAccessGroupInitiatorReq(
                    volumeAccessGroupId = req.volumeAccessGroupId,
                    clusterId = req.clusterId,
                    initiatorId = req.id
                ))
                call.respondOK()
            }
        }
    }
    handleDataPost("/volume-access-group/delete"){
        ifFromPortalPage{ _, portal ->
            val req = call.readDataRequest<DeleteVolumeAccessGroupReq>()
            httpOperationScope(portal){
                create<DeleteVolumeAccessGroupOperation>().invoke(req)
            }
            call.respondOK()
        }
    }
}

@kotlinx.serialization.Serializable
data class UpdateVolumeAccessGroupRequest(
    val clusterId: String,
    val volumeAccessGroupId: String,
    val id: String
)