package server.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import operation.cluster.ListClusterOperation
import operation.cluster.ListClusterReq
import operation.httpOperationScope
import operation.volume.ListVolumeOperation
import operation.volume.ListVolumeReq
import server.handleDataPost
import server.ifFromPortalPage
import server.respondTraceable
import utils.OperationExecutor
import utils.VolumeInfo

fun Routing.volumeRoute() {

}