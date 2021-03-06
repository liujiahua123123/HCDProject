package server.route

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import operation.cluster.ListClusterOperation
import operation.cluster.ListClusterReq
import operation.httpOperationScope
import operation.task.TraceTaskOperation
import operation.task.TraceTaskReq
import operation.volume.*
import server.*
import utils.OperationExecutor
import utils.VolumeInfo


@kotlinx.serialization.Serializable
data class CreateMultiVolumeRequest(val input: CreateVolumeReq, val count: Int)

fun Routing.volumeRoute() {
    handleDataPost("/volume/create") {
        ifFromPortalPage { user, portal ->
            val req = call.readDataRequest<CreateVolumeReq>()
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit> {
                httpOperationScope(portal) {
                    updateProgress(1, 2, "Creating volume")
                    val task = create<CreateVolumeOperation>().invoke(req).taskId
                    updateProgress("Waiting to complete")
                    while (create<TraceTaskOperation>().invoke(TraceTaskReq(task)).progress != 100) {
                        delay(500)
                    }
                }
            })
        }
    }

    handleDataPost("/volume/create-multi") {
        ifFromPortalPage { user, portal ->
            val req = call.readDataRequest<CreateMultiVolumeRequest>()

            if (!req.input.volumeName.contains("{num}")) {
                userInputError("Must have {num} in name field for multi-create")
            }

            call.respondTraceable(OperationExecutor.addExecutorTask<Unit> {
                httpOperationScope(portal) {
                    updateProgress(0, req.count, "Creating volume")

                    repeat(req.count) {
                        val task = create<CreateVolumeOperation>().invoke(
                            req.input.copy(
                                volumeName = req.input.volumeName.replace("{num}", "$it")
                            )
                        ).taskId
                        updateProgress("Creating..")
                        while (create<TraceTaskOperation>().invoke(TraceTaskReq(task)).progress != 100) {
                            delay(500)
                        }
                    }
                }
            })
        }
    }

    handleDataPost("/volume/delete") {
        ifFromPortalPage { user, portal ->
            val req = call.readDataRequest<List<SelectedVolume>>()
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit> {
                httpOperationScope(portal) {
                    for (i in req.indices) {
                        updateProgress(i, req.size, "Deleting volume " + req[i].volumeId)
                        create<DeleteVolumeOperation>().invoke(DeleteVolumeReq(req[i].volumeId, req[i].clusterId))
                    }
                }
            })
        }
    }


}

@kotlinx.serialization.Serializable
data class SelectedVolume(val volumeId: String, val clusterId: String)
