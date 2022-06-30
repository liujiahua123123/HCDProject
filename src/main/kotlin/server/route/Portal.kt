package server.route

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import operation.cluster.ListClusterOperation
import operation.cluster.ListClusterReq
import operation.host.ListDiskByHostOperation
import operation.host.ListDiskByHostReq
import operation.host.ListHostOperation
import operation.host.ListHostReq
import operation.httpOperationScope
import operation.initiator.ListInitiatorOperation
import operation.initiator.ListInitiatorReq
import operation.volume.ListVolumeOperation
import operation.volume.ListVolumeReq
import server.handleDataPost
import server.ifFromPortalPage
import server.ifLogin
import server.respondTraceable
import utils.*
import java.util.*

fun Routing.portalRoute(){
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

    handleDataPost("/portal/refresh"){
        ifFromPortalPage { user, portal ->
            call.respondTraceable(OperationExecutor.addExecutorTask<PortalRefreshResult>{
                httpOperationScope(portal){
                    updateProgress(0,4,"Synchronizing Data")
                    val clusters = create<ListClusterOperation>().invoke(ListClusterReq()).data

                    val context = CoroutineScope(Job())

                    val hostsResult = context.async {
                        val result = Collections.synchronizedList(mutableListOf<ClusterWithHosts>())
                        val hosts = create<ListHostOperation>().invoke(ListHostReq(clusterId = null, onlyFreeHosts = false)).data
                        for (i in hosts.indices) {
                            val group = result.firstOrNull { it.clusterId == hosts[i].clusterId } ?: ClusterWithHosts(
                                hosts[i].clusterId,
                                Collections.synchronizedList(mutableListOf())
                            ).apply {
                                result.add(this)
                            }
                            group.hosts.add(
                                HostWithDisks(
                                    hosts[i].hostId, hosts[i].hostName, hosts[i].clusterId, hosts[i].state,
                                    disks = create<ListDiskByHostOperation>().invoke(ListDiskByHostReq(hostId = hosts[i].hostId)).data
                                )
                            )
                        }

                        updateProgress("Synchronizing Data")
                        result.sortedWith { o1, o2 ->
                            if (o1.clusterId == null) {
                                Int.MIN_VALUE
                            } else if (o2.clusterId == null) {
                                Int.MAX_VALUE
                            } else {
                                o1.clusterId.compareTo(o2.clusterId)
                            }
                        }
                    }

                    val volumesResult = context.async{
                        val volumesResult = mutableMapOf<String, List<VolumeInfo>>()

                        clusters.forEach {
                            volumesResult[it.clusterId] = create<ListVolumeOperation>().invoke(ListVolumeReq(it.clusterId)).data
                        }
                        updateProgress("Synchronizing Data")
                        volumesResult
                    }


                    val templateResult = context.async {
                        val templatesResult = mutableListOf<TemplateDigest>()
                        user.dataScope<ClusterTemplate> { all ->
                            all.filter { it.portal == portal }.forEach {
                                templatesResult.add(TemplateDigest(
                                    id = it.id,
                                    name = it.templateName,
                                    info = buildString {
                                        append("Will create cluster: ")
                                        append(it.creator.clusterName)
                                        append(" with IP=")
                                        append(it.creator.virtualIp)
                                        append(", Replication=")
                                        append(it.creator.replicationFactor)
                                        append(", MinClusterSize=")
                                        append(it.creator.minClusterSize)
                                        append("\n")
                                        append("Involve Hosts: ")
                                        append(it.hosts.keys.joinToString(","))
                                        append("\n")
                                        append("Involve Volumes: ")
                                        append("TODO")
                                    }
                                ))
                            }
                            false
                        }
                        updateProgress("Synchronized Data")
                        templatesResult
                    }

                    val initiators = context.async {
                        create<ListInitiatorOperation>().invoke(ListInitiatorReq()).data.also {
                            updateProgress("Synchronizing Data")
                        }

                    }

                    PortalRefreshResult(
                        hosts=hostsResult.await(),
                        volumes=volumesResult.await(),
                        templates=templateResult.await(),
                        initiators=initiators.await()
                    )
                }
            })
        }
    }
}

@kotlinx.serialization.Serializable
data class TemplateDigest(
    val name: String,
    val info: String,
    val id: String
)


@kotlinx.serialization.Serializable
data class PortalRefreshResult(
    val hosts: List<ClusterWithHosts>,
    val volumes: Map<String, List<VolumeInfo>>,
    val templates: List<TemplateDigest>,
    val initiators: List<Initiator>
)