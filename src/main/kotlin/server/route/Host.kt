package server.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.supervisorScope
import operation.host.*
import operation.httpOperationScope
import server.handleDataPost
import server.ifFromPortalPage
import server.respondTraceable
import utils.DiskInfo
import utils.OperationExecutor
import java.util.Collections

fun Routing.hostRoute() {

    handleDataPost("/host/list") {
        ifFromPortalPage { _, portal ->
            call.respondTraceable(OperationExecutor.addExecutorTask<List<HostWithDisks>>(numOfStep = 2) {
                httpOperationScope(portal) {
                    updateProgress("Listing Hosts")

                    val result = Collections.synchronizedList(mutableListOf<HostWithDisks>())
                    val hosts = create<ListHostOperation>().invoke(ListHostReq(clusterId = null, onlyFreeHosts = false)).data

                    for (i in hosts.indices) {
                        updateProgress(i + 1, hosts.size, "Listing disks under ${hosts[i].hostName}")
                        result.add(
                            HostWithDisks(
                                hosts[i].hostId, hosts[i].hostName, hosts[i].clusterId, hosts[i].state,
                                disks = create<ListDiskByHostOperation>().invoke(ListDiskByHostReq(hostId = hosts[i].hostId)).data
                            )
                        )
                    }

                    result
                }
            })
        }
    }

    handleDataPost("/host/list-by-cluster"){
        ifFromPortalPage { _, portal ->
            call.respondTraceable(OperationExecutor.addExecutorTask<List<ClusterWithHosts>>(numOfStep = 2) {
                httpOperationScope(portal) {
                    updateProgress(0,1,"Listing Hosts")

                    val result = Collections.synchronizedList(mutableListOf<ClusterWithHosts>())
                    val hosts = create<ListHostOperation>().invoke(ListHostReq(clusterId = null, onlyFreeHosts = false)).data
                    for (i in hosts.indices) {
                        updateProgress(i + 1, hosts.size, "Listing disks under ${hosts[i].hostName}")
                        val group = result.firstOrNull { it.clusterId == hosts[i].clusterId }?: ClusterWithHosts(hosts[i].clusterId,
                            Collections.synchronizedList(mutableListOf())
                        ).apply {
                            result.add(this)
                        }
                        group.hosts.add(HostWithDisks(
                            hosts[i].hostId, hosts[i].hostName, hosts[i].clusterId, hosts[i].state,
                            disks = create<ListDiskByHostOperation>().invoke(ListDiskByHostReq(hostId = hosts[i].hostId)).data
                        ))
                    }

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
            })
        }
    }
}

@kotlinx.serialization.Serializable
data class HostWithDisks(
    val hostId: String,
    val hostName: String,
    val clusterId: String?,
    val state: String,
    val disks: List<DiskInfo>
)

@kotlinx.serialization.Serializable
data class ClusterWithHosts(
    val clusterId: String?,
    val hosts: MutableList<HostWithDisks>

)