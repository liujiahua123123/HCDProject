package server.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import operation.cluster.CreateClusterInfo
import operation.cluster.CreateClusterOperation
import operation.cluster.CreateClusterReq
import operation.host.*
import operation.httpOperationScope
import operation.task.TraceTaskOperation
import operation.task.TraceTaskReq
import server.handleDataPost
import server.ifFromPortalPage
import server.readDataRequest
import server.respondTraceable
import utils.DiskInfo
import utils.OperationExecutor
import java.util.Collections

fun Routing.hostRoute() {
    handleDataPost("/host/list") {
        ifFromPortalPage { _, portal ->
            call.respondTraceable(OperationExecutor.addExecutorTask<List<HostWithDisks>> {
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