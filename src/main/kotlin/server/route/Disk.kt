package server.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import operation.HttpOperationBuilder
import operation.disk.DiskAddTagOperation
import operation.disk.DiskAddTagReq
import operation.disk.DiskRemoveTagOperation
import operation.disk.DiskRemoveTagReq
import operation.host.ListDiskByHostOperation
import operation.host.ListDiskByHostReq
import operation.host.ListHostOperation
import operation.host.ListHostReq
import operation.httpOperationScope
import operation.task.TraceTaskOperation
import operation.task.TraceTaskReq
import server.*
import utils.DiskInfo
import utils.OperationExecutor
import java.util.*

fun Routing.diskRoute() {

    handleDataPost("/disk/addtag") {
        ifFromPortalPage { user, portal ->
            val request = call.readDataRequest<EditTagRequest>()
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit>(1) {
                httpOperationScope(portal) {
                    updateProgress(0, request.disks.size * 3, "Verify Request")

                    val cache = mutableMapOf<String, List<DiskInfo>>()
                    val error = StringBuilder()

                    request.disks.forEachIndexed { index, editTagRequestDiskInfo ->
                        updateProgress("Verify Add Request For " + editTagRequestDiskInfo.diskId)

                        if (cache[editTagRequestDiskInfo.hostId] == null) {
                            cache[editTagRequestDiskInfo.hostId] =
                                create<ListDiskByHostOperation>().invoke(ListDiskByHostReq(hostId = editTagRequestDiskInfo.hostId)).data
                        }
                        val diskTags =
                            cache[editTagRequestDiskInfo.hostId]!!.firstOrNull { it.diskId == editTagRequestDiskInfo.diskId }?.diskTagList
                                ?: userInputError("Couldn't find " + editTagRequestDiskInfo.diskId + " under " + editTagRequestDiskInfo.hostId)

                        if (!request.ignoreUnqualifiedPairs) {
                            for (tag in request.tags) {
                                if (tag in diskTags) {
                                    error.appendLine(tag + " already found under disk " + editTagRequestDiskInfo.diskId)
                                }
                            }
                        }

                        diskTags.addAll(request.tags)
                        if ((diskTags.contains("DATA_DISK") || diskTags.contains("META_DATA")) && diskTags.contains("WRITE_CACHE")) {
                            error.appendLine("DATA, META, WRITE_CACHE are not compatible for disk " + editTagRequestDiskInfo.diskId)
                        }

                        //backtrack
                        diskTags.removeAll(request.tags)
                    }
                    if (error.isNotEmpty()) {
                        userInputError(error)
                    }

                    val taskList = Collections.synchronizedList(mutableListOf<String>())

                    request.disks.forEachIndexed { index, editTagRequestDiskInfo ->
                        updateProgress("Adding Tag on " + editTagRequestDiskInfo.diskId)
                        for (tag in request.tags) {
                            if (request.ignoreUnqualifiedPairs) {
                                val info =
                                    cache[editTagRequestDiskInfo.hostId]!!.first { it.diskId == editTagRequestDiskInfo.diskId }
                                if (tag in info.diskTagList) {
                                    continue
                                }
                            }
                            taskList.add(
                                create<DiskAddTagOperation>().invoke(
                                    DiskAddTagReq(
                                        hostId = editTagRequestDiskInfo.hostId,
                                        diskIds = listOf(editTagRequestDiskInfo.diskId),
                                        diskTag = tag
                                    )
                                ).taskId
                            )
                        }
                    }

                    if (request.traceUntilComplete) {
                        traceUntilComplete(this@httpOperationScope, request, taskList)
                    }
                }
            })
        }
    }

    handleDataPost("/disk/removetag") {
        ifFromPortalPage { user, portal ->
            val request = call.readDataRequest<EditTagRequest>()
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit>(1) {
                httpOperationScope(portal) {
                    updateProgress(0, request.disks.size * 3, "Verify Request")

                    val cache = mutableMapOf<String, List<DiskInfo>>()
                    val error = StringBuilder()
                    request.disks.forEachIndexed { index, editTagRequestDiskInfo ->
                        updateProgress("Verify Remove Request For " + editTagRequestDiskInfo.diskId)
                        if (cache[editTagRequestDiskInfo.hostId] == null) {
                            cache[editTagRequestDiskInfo.hostId] =
                                create<ListDiskByHostOperation>().invoke(ListDiskByHostReq(hostId = editTagRequestDiskInfo.hostId)).data
                            val host =
                                create<ListHostOperation>().invoke(ListHostReq()).data.firstOrNull { it.hostId == editTagRequestDiskInfo.hostId }
                                    ?: userInputError("Couldn't find " + editTagRequestDiskInfo.hostId + " under " + portal)
                            if (host.clusterId != null) {
                                error.appendLine(host.hostId + " already under cluster " + host.clusterId)
                            }
                        }
                        val diskTags =
                            cache[editTagRequestDiskInfo.hostId]!!.firstOrNull { it.diskId == editTagRequestDiskInfo.diskId }?.diskTagList
                                ?: userInputError("Couldn't find " + editTagRequestDiskInfo.diskId + " under " + editTagRequestDiskInfo.hostId)

                        if (!request.ignoreUnqualifiedPairs) {
                            request.tags.forEach {
                                if (it !in diskTags) {
                                    error.appendLine(it + " not found under " + editTagRequestDiskInfo.diskId)
                                }
                            }
                        }
                    }

                    if (error.isNotEmpty()) {
                        userInputError(error)
                    }

                    val taskList = Collections.synchronizedList(mutableListOf<String>())
                    //can be concurrent
                    request.disks.forEachIndexed { index, editTagRequestDiskInfo ->
                        updateProgress("Removing Tag on " + editTagRequestDiskInfo.diskId)
                        for (tag in request.tags) {
                            if (request.ignoreUnqualifiedPairs) {
                                val info =
                                    cache[editTagRequestDiskInfo.hostId]!!.first { it.diskId == editTagRequestDiskInfo.diskId }
                                if (tag !in info.diskTagList) {
                                    continue
                                }
                            }
                            taskList.add(
                                create<DiskRemoveTagOperation>().invoke(
                                    DiskRemoveTagReq(
                                        hostId = editTagRequestDiskInfo.hostId,
                                        diskIds = listOf(editTagRequestDiskInfo.diskId),
                                        diskTag = tag
                                    )
                                ).taskId
                            )
                        }
                    }

                    if (request.traceUntilComplete) {
                        traceUntilComplete(this@httpOperationScope, request, taskList)
                    }
                }
            })
        }
    }
}


private suspend fun utils.Job<Unit>.traceUntilComplete(
    scope: HttpOperationBuilder,
    request: EditTagRequest,
    taskList: MutableList<String>
) {
    //can be concurrent
    val totalSize = taskList.size
    while (taskList.size != 0) {
        val poll = taskList.first()
        updateProgress(
            request.disks.size * 2 + (((totalSize - taskList.size) * request.disks.size) / totalSize),
            request.disks.size * 3,
            "Tracing Task $poll"
        )
        if (scope.create<TraceTaskOperation>().invoke(
                TraceTaskReq(
                    taskId = poll
                )
            ).progress == 100
        ) {
            taskList.remove(poll)
            continue
        }
        delay(500)
    }
}


@kotlinx.serialization.Serializable
data class EditTagRequest(
    val tags: List<String>,
    val disks: List<EditTagRequestDiskInfo>,
    val traceUntilComplete: Boolean,
    val ignoreUnqualifiedPairs: Boolean
) {
    @kotlinx.serialization.Serializable
    data class EditTagRequestDiskInfo(
        val hostId: String,
        val diskId: String
    )
}