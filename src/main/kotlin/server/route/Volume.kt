package server.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import operation.cluster.ListClusterOperation
import operation.cluster.ListClusterReq
import operation.httpOperationScope
import operation.volume.CreateVolumeReq
import operation.volume.ListVolumeOperation
import operation.volume.ListVolumeReq
import server.handleDataPost
import server.ifFromPortalPage
import server.readDataRequest
import server.respondTraceable
import utils.OperationExecutor
import utils.VolumeInfo

fun Routing.volumeRoute() {
    handleDataPost("/volume/create"){
        ifFromPortalPage { user, portal ->
            call.readDataRequest<CreateVolumeReq>()
        }
    }
}