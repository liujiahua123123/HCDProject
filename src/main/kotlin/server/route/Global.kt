package server.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import operation.cluster.CreateClusterOperation
import operation.cluster.CreateClusterReq
import operation.disk.DiskAddTagOperation
import operation.disk.DiskAddTagReq
import operation.host.ListDiskByHostOperation
import operation.host.ListDiskByHostReq
import operation.host.ListHostOperation
import operation.host.ListHostReq
import operation.httpOperationScope
import operation.task.TraceTaskOperation
import operation.task.TraceTaskReq
import org.apache.sshd.client.SshClient
import server.*
import ssh.HCDSshClient
import utils.ClusterTemplate
import utils.OperationExecutor
import utils.dataScope
import utils.getAllData

fun Routing.globalRoute() {

    handleDataPost("/purge-db") {
        ifFromPortalPage { user, portal ->
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit> {
                updateProgress(0, 1, "Allocating Host connection points")
                val address = ListHostOperation().apply { this.portal = portal }.invoke(ListHostReq()).data

                updateProgress(1, address.size + 4, "Making SSH Connections")
                val list = address.map {
                    HCDSshClient(
                        name = it.hostName,
                        address = it.managementAddress!!
                    )
                }

                updateProgress("Disabling Services (Concurrent)")
                supervisorScope {
                    list.forEach {
                        launch {
                            it.execute("sudo systemctl stop hcdmgmt")
                            it.execute("sudo systemctl stop hcdadmin")
                        }
                    }
                }
                delay(5000)


                list.forEach {
                    updateProgress("Running db_purge.sh on " + it.name)
                    it.execute("sudo /usr/share/hcdinstall/scripts/db_purge.sh")
                }
                delay(5000)

                updateProgress("Running db_config.sh (Concurrent)")
                supervisorScope {
                    list.forEach {
                        launch {
                            it.execute("sudo /usr/share/hcdinstall/scripts/db_config.sh")
                        }
                    }
                }
                delay(5000)

                updateProgress("Starting All Services")
                supervisorScope {
                    list.forEach {
                        launch {
                            it.execute("sudo systemctl start hcdmgmt")
                            it.execute("sudo systemctl start hcdadmin")
                        }
                    }
                }
                delay(8000)
            })
        }
    }

    handleDataPost("/available-template") {
        ifFromPortalPage { user, portal ->
            val list = mutableListOf<TemplateDigest>()
            user.dataScope<ClusterTemplate> { all ->
                all.filter { it.portal == portal }.forEach {
                    list.add(TemplateDigest(
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
            call.respondOK(list)
        }
    }


    handleDataPost("/apply-template") {
        ifFromPortalPage { user, portal ->
            val request = call.readDataRequest<ApplyTemplateRequest>()
            val templateId = request.templateId
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit> {
                httpOperationScope(portal) {
                    updateProgress(0, 6, "Retrieving Template")
                    val hosts = create<ListHostOperation>().invoke(ListHostReq()).data

                    val template =
                        user.getAllData<ClusterTemplate>().firstOrNull { it.id == templateId } ?: userInputError("Failed to find $templateId")
                    updateProgress(1, 6, "Retrieving Hosts")

                    val hostTemplates = template.hosts

                    val allJobs = mutableSetOf<String>()
                    val hostToJoin = mutableListOf<String>()

                    hostTemplates.forEach { hostLevel ->
                        updateProgress(2, 6, "Setting up host " + hostLevel.key)
                        val info = hosts.firstOrNull { it.hostName == hostLevel.key }
                        if (info != null && info.clusterId == null) {
                            //disk
                            val hostId = info.hostId
                            val diskInfos = create<ListDiskByHostOperation>().invoke(ListDiskByHostReq(hostId)).data

                            hostLevel.value.disks.forEach { diskLevel ->
                                val diskInfo = diskInfos.firstOrNull { it.diskSerialNumber == diskLevel.key }

                                if (diskInfo != null) {
                                    val diskId = diskInfo.diskId
                                    val currTags = diskInfo.diskTagList

                                    diskLevel.value.tags.forEach { tag ->
                                        if (tag !in currTags) {
                                            allJobs.add(
                                                create<DiskAddTagOperation>().invoke(
                                                    DiskAddTagReq(
                                                        hostId, mutableListOf(diskId), tag
                                                    )
                                                ).taskId
                                            )
                                            delay(1000)
                                        }
                                    }
                                }
                            }
                            hostToJoin.add(hostId)
                        }
                    }
                    updateProgress(3, 6, "Setting up volumes")




                    val allTask = allJobs.size
                    while (allJobs.isNotEmpty()) {
                        updateProgress(4, 6, "Waiting for all task to complete (" + (allTask - allJobs.size) + "/" + allTask + ")")
                        val job = allJobs.first()
                        if(create<TraceTaskOperation>().invoke(TraceTaskReq(job)).progress == 100){
                            allJobs.remove(job)
                        }else{
                            delay(444)
                        }
                    }

                    if(hostToJoin.isEmpty()){
                        userInputError("No any host are ready to join cluster")
                    }

                    updateProgress(5, 6, "Setting up Cluster")
                    val id = create<CreateClusterOperation>().invoke(
                        CreateClusterReq(
                            cluster = template.creator,
                            hosts = hostToJoin
                        )
                    ).taskId
                    while (true){
                        if(create<TraceTaskOperation>().invoke(TraceTaskReq(id)).progress == 100){
                            break
                        }
                        delay(444)
                    }

                    updateProgress(6, 6, "Sync")
                    delay(8000)
                }
            })

        }
    }

}


@kotlinx.serialization.Serializable
data class ApplyTemplateRequest(
    val templateId: String
)

@kotlinx.serialization.Serializable
data class TemplateDigest(
    val name: String,
    val info: String,
    val id: String
)
