package server.route

import io.ktor.html.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import operation.HttpOperationBuilder
import operation.cluster.*
import operation.disk.DiskAddTagOperation
import operation.disk.DiskAddTagReq
import operation.host.ListDiskByHostOperation
import operation.host.ListDiskByHostReq
import operation.host.ListHostOperation
import operation.host.ListHostReq
import operation.httpOperationScope
import operation.initiator.CreateInitiatorOperation
import operation.initiator.CreateInitiatorReq
import operation.initiator.ListInitiatorOperation
import operation.initiator.ListInitiatorReq
import operation.task.TraceTaskOperation
import operation.task.TraceTaskReq
import operation.volume.CreateVolumeOperation
import operation.volume.CreateVolumeReq
import operation.volume.ListVolumeOperation
import operation.volume.ListVolumeReq
import operation.volumeaccessgroup.CreateVolumeAccessGroupOperation
import operation.volumeaccessgroup.CreateVolumeAccessGroupReq
import operation.volumeaccessgroup.ListVolumeAccessGroupOperation
import operation.volumeaccessgroup.ListVolumeAccessGroupReq
import server.*
import server.trace.Traceable
import ssh.HCDSshClient
import utils.*

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

    handleDataPost("/delete-template") {
        ifFromPortalPage { user, portal ->
            val request = call.readDataRequest<TemplateRequest>()
            user.dataScope<ClusterTemplate> {
                it.removeIf { e -> e.id == request.templateId }
            }
            call.respondOK()
        }
    }

    handleDataPost("save-template") {
        ifFromPortalPage { user, portal ->
            val request = call.readDataRequest<SaveTemplateRequest>()
            val requestCluster = request.clusterId
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit> {
                httpOperationScope(portal) {
                    updateProgress(1, 8, "Saving Cluster Info")
                    val data =
                        create<ListClusterOperation>().invoke(ListClusterReq()).data.firstOrNull { it.clusterId == requestCluster } ?: userInputError(
                            "cluster id not found"
                        )
                    val req = CreateClusterInfo(
                        clusterName = data.clusterName,
                        minClusterSize = data.minClusterSize,
                        replicationFactor = data.replicationFactor,
                        virtualIp = data.virtualIp
                    )
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
                    updateProgress("Saving Initiator")
                    val initiatorMap = create<ListInitiatorOperation>().invoke(ListInitiatorReq()).data.associate { it.iqn to it.initiatorName }
                    updateProgress("Saving Volume Access Group")
                    val groups = create<ListVolumeAccessGroupOperation>().invoke(ListVolumeAccessGroupReq(clusterId = requestCluster)).data
                    updateProgress("Saving Volumes")
                    val volumes = create<ListVolumeOperation>().invoke(ListVolumeReq(clusterId = requestCluster)).data
                    updateProgress("Wrapping and Saving")
                    user.dataScope<ClusterTemplate> {
                        it.removeIf { x -> x.templateName == request.name }
                        it.add(ClusterTemplate(
                            creator = req,
                            portal = portal,
                            hosts = hostToSave,
                            templateName = request.name,
                            initiators = groups.map { inner ->
                                inner.initiators.map { ido ->
                                    InitiatorTemplate(
                                        initiatorMap[ido.iqn]!!, ido.iqn
                                    )
                                }
                            }.flatten().distinct(),
                            volumes = volumes.map { inner ->
                                VolumeTemplate(
                                    volumeName = inner.volumeName,
                                    volumeSize = inner.volumeSize,
                                    type = inner.type,
                                    blockSize = inner.blockSize
                                )
                            },
                            volumeAccessGroups = groups.map { inner ->
                                VolumeAccessGroupTemplate(
                                    name = inner.volumeAccessGroupName,
                                    initiators = inner.initiators.map { initiatorMap[it.iqn]!! },
                                    volumes = inner.volumes.map { it.name },
                                )
                            }
                        ))
                        true
                    }

                    updateProgress("Sync")
                    delay(1000)
                }
            })
        }
    }


    suspend fun Job<Unit>.traceAllUntilComplete(scope: HttpOperationBuilder, allJobs: MutableSet<String>){
        val allTask = allJobs.size
        while (allJobs.isNotEmpty()) {
            updateProgressDescription("Waiting for all task to complete (" + (allTask - allJobs.size) + "/" + allTask + ")")
            val job = allJobs.first()
            if (scope.create<TraceTaskOperation>().invoke(TraceTaskReq(job)).progress == 100) {
                allJobs.remove(job)
            } else {
                delay(444)
            }
        }
    }

    handleDataPost("/apply-template") {
        ifFromPortalPage { user, portal ->
            val request = call.readDataRequest<TemplateRequest>()
            val templateId = request.templateId
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit> {
                httpOperationScope(portal) {
                    updateProgress(0, 9, "Retrieving Template")
                    val hosts = create<ListHostOperation>().invoke(ListHostReq()).data

                    val template =
                        user.getAllData<ClusterTemplate>().firstOrNull { it.id == templateId } ?: userInputError("Failed to find $templateId")
                    updateProgress("Retrieving Hosts")

                    val hostTemplates = template.hosts

                    val allJobs = mutableSetOf<String>()
                    val hostToJoin = mutableListOf<String>()

                    hostTemplates.forEach { hostLevel ->
                        updateProgressDescription("Setting up host " + hostLevel.key)
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

                    traceAllUntilComplete(this, allJobs)

                    if (hostToJoin.isEmpty()) {
                        userInputError("No any host are ready to join cluster")
                    }

                    updateProgress("Setting up Cluster")
                    val id = create<CreateClusterOperation>().invoke(
                        CreateClusterReq(
                            cluster = template.creator,
                            hosts = hostToJoin
                        )
                    ).taskId
                    traceAllUntilComplete(this, hashSetOf(id))

                    updateProgress("Syncing")
                    delay(8000)

                    val clusterId = create<ListClusterOperation>().invoke(ListClusterReq()).data.firstOrNull { it.clusterName == template.creator.clusterName }?.clusterId?:userInputError("Failed to find the created cluster")

                    updateProgress("Setting up Initiators")
                    val currInitiators = create<ListInitiatorOperation>().invoke(ListInitiatorReq()).data.associate{ it.initiatorName to it.initiatorId }
                    template.initiators.forEach {
                        if(currInitiators[it.name] == null){
                            create<CreateInitiatorOperation>().invoke(CreateInitiatorReq(it.name, it.iqn))
                        }
                    }
                    delay(3000)
                    val initiatorNameToIdMap = create<ListInitiatorOperation>().invoke(ListInitiatorReq()).data.associate{ it.initiatorName to it.initiatorId }

                    updateProgress("Setting up Volumes")
                    val tasks = mutableSetOf<String>()
                    template.volumes.forEach {
                        tasks.add(create<CreateVolumeOperation>().invoke(CreateVolumeReq(it.volumeName, clusterId, it.volumeSize, it.blockSize, it.type)).taskId)
                    }

                    traceAllUntilComplete(this, tasks)
                    updateProgress("Syncing")
                    delay(3000)
                    val volumeNameToIdMap = create<ListVolumeOperation>().invoke(ListVolumeReq(clusterId)).data.associate { it.volumeName to it.volumeId }

                    updateProgress("Setting up Volume Access Groups")
                    template.volumeAccessGroups.forEach {
                        create<CreateVolumeAccessGroupOperation>().invoke(
                            CreateVolumeAccessGroupReq(
                                it.name,
                                clusterId,
                                it.initiators.map { name -> IdObject(id=initiatorNameToIdMap[name]?: userInputError("Can not found created $name")) },
                                it.volumes.map {  name -> IdObject(id=volumeNameToIdMap[name]?: userInputError("Can not found created $name"))}
                            )
                        )
                    }

                    updateProgress("Sync")
                    delay(3000)
                }
            })

        }
    }

}


@kotlinx.serialization.Serializable
data class TemplateRequest(
    val templateId: String
)

