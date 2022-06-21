package server.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import operation.HttpOperationBuilder
import operation.cluster.*
import operation.host.ListHostOperation
import operation.host.ListHostReq
import operation.httpOperationScope
import operation.task.TraceTaskOperation
import operation.task.TraceTaskReq
import server.*
import utils.Job
import utils.OperationExecutor

fun Routing.clusterRoute() {
    handleDataPost("/cluster/new"){
        ifFromPortalPage { _, portal ->
            val request = call.readDataRequest<CreateClusterReq>()
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit>{
                httpOperationScope(portal) {
                    updateProgress(0,3,"Creating")
                    val id = create<CreateClusterOperation>().invoke(request).taskId
                    updateProgress("Waiting for task $id")
                    while (true){
                        if(create<TraceTaskOperation>().invoke(TraceTaskReq(taskId = id)).progress == 100){
                            break
                        }
                        delay(500)
                    }
                    updateProgress("Sync")
                    delay(3939)//restful server issue
                }
            })
        }
    }

    handleDataPost("/cluster/delete"){
        ifFromPortalPage { _, portal ->
            val request = call.readDataRequest<DeleteClusterRequest>()
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit>{
                httpOperationScope(portal){
                    updateProgress(0,request.clusters.size * 2 + 1,"Deleting")
                    val list = mutableListOf<String>()
                    request.clusters.forEach {
                        updateProgress("Deleting $it")
                        list.add(create<DeleteClusterOperation>().invoke(DeleteClusterReq(it)).taskId)
                    }
                    trackUntilComplete(request.clusters.size,list,this)
                }
            })
        }
    }

    handleDataPost("/cluster/expand"){
        ifFromPortalPage { _, portal ->

        }
    }

    handleDataPost("/cluster/remove-host"){
        ifFromPortalPage {_, portal ->
            val request = call.readDataRequest<RemoveHostFromClusterRequest>()
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit> {
                httpOperationScope(portal) {
                    updateProgress(0,request.hosts.size * 2, "Listing Hosts")
                    val hosts = create<ListHostOperation>().invoke(ListHostReq()).data.filter { it.hostId in request.hosts && it.clusterId != null }
                    if(hosts.size != request.hosts.size){
                        userInputError("Can't remove host that not belong to any cluster")
                    }
                    val list = mutableListOf<String>()
                    hosts.forEach {
                        updateProgress("Removing ${it.hostId}")
                        list.add(create<RemoveHostFromClusterOperation>().invoke(RemoveHostFromClusterReq(
                            clusterId = it.clusterId!!,
                            hostIpAddress = it.managementAddress!!,
                            hostId = it.hostId
                        )).taskId)
                    }
                    trackUntilComplete(request.hosts.size,list,this)
                }
            })
        }
    }

}

suspend fun Job<*>.trackUntilComplete(totalSize: Int, list: MutableList<String>, scope:HttpOperationBuilder){
    while (list.isNotEmpty()){
        val poll = list.first()
        updateProgress(totalSize * 2 - list.size, totalSize * 2 + 1, "Tracking $poll")
        if(scope.create<TraceTaskOperation>().invoke(TraceTaskReq(poll)).progress == 100){
            list.remove(poll)
            continue
        }
        delay(555)
    }
    updateProgress("Sync")
    delay(3939)
}

@kotlinx.serialization.Serializable
data class DeleteClusterRequest(
    val clusters: List<String>
)

@kotlinx.serialization.Serializable
data class RemoveHostFromClusterRequest(
    val hosts: List<String>
)
