package server.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import operation.HttpOperationBuilder
import operation.cluster.*
import operation.host.ListDiskByHostOperation
import operation.host.ListDiskByHostReq
import operation.host.ListHostOperation
import operation.host.ListHostReq
import operation.httpOperationScope
import operation.task.TraceTaskOperation
import operation.task.TraceTaskReq
import server.*
import utils.*

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

    handleDataPost("/cluster/save"){
        ifFromPortalPage { user, portal ->
            val request = call.readDataRequest<SaveTemplateRequest>()
            val requestCluster = request.clusterId
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit> {
                httpOperationScope(portal){
                    updateProgress(1,5,"Saving Cluster Info")
                    val data = create<ListClusterOperation>().invoke(ListClusterReq()).data.firstOrNull { it.clusterId == requestCluster }?: userInputError("cluster id not found")
                    val req = CreateClusterInfo(clusterName = data.clusterName, minClusterSize = data.minClusterSize, replicationFactor = data.replicationFactor, virtualIp = data.virtualIp)
                    updateProgress("Saving Host Info")
                    val hosts = create<ListHostOperation>().invoke(ListHostReq(clusterId = requestCluster)).data

                    updateProgress("Saving Disk Info")
                    val hostToSave = mutableMapOf<String, HostTemplate>()
                    hosts.forEach {
                        hostToSave[it.hostName] = HostTemplate(
                            disks = create<ListDiskByHostOperation>().invoke(ListDiskByHostReq(hostId = it.hostId)).data.associate { disk ->
                                Pair(disk.diskSerialNumber, DiskTemplate(disk.diskTagList))
                            }
                        )
                    }

                    updateProgress("Wrapping and Saving")
                    user.dataScope<ClusterTemplate> {
                        it.removeIf { x -> x.templateName == request.name }
                        it.add(ClusterTemplate(
                            creator = req,
                            portal = portal,
                            hosts = hostToSave,
                            templateName = request.name,
                        ))
                        true
                    }

                    updateProgress("Sync")
                    delay(3000)
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

@kotlinx.serialization.Serializable
data class SaveTemplateRequest(
    val clusterId: String,
    val name: String,
)