package server.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import operation.httpOperationScope
import operation.initiator.CreateInitiatorOperation
import operation.initiator.DeleteInitiatorOperation
import operation.initiator.DeleteInitiatorReq
import server.*
import utils.OperationExecutor

fun Routing.initiatorRoute() {
    handleDataPost("/initiator/create") {
        ifFromPortalPage { user, portal ->
            httpOperationScope(portal) {
                create<CreateInitiatorOperation>().invoke(call.readDataRequest())
            }
            call.respondOK()
        }
    }

    handleDataPost("/initiator/delete") {
        val req = call.readDataRequest<List<String>>()
        ifFromPortalPage { user, portal ->
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit> {
                updateProgress(0, req.size, "Deleting initiators")
                httpOperationScope(portal) {
                    req.forEach {
                        updateProgress("Deleting initiator $it")
                        create<DeleteInitiatorOperation>().invoke(DeleteInitiatorReq(it))
                    }
                }
            })
        }
    }
}

