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

fun Routing.hostRoute() {

    get("/portal/{portal}") {

    }

    handleDataPost("/list") {
        ifFromPortalPage { _, portal ->
            call.respondTraceable(OperationExecutor.addExecutorTask<List<HostWithDisks>>(numOfStep = 2) {
                httpOperationScope(portal) {
                    updateProgress("Listing Hosts")

                    val result = mutableListOf<HostWithDisks>()
                    val hosts =
                        create<ListHostOperation>().invoke(ListHostReq(clusterId = null, onlyFreeHosts = false)).data

                    //can be async
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