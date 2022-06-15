package utils

import kotlinx.serialization.Serializable

@Serializable
data class HostInfo(
    val hostId: String,
    val hostName: String,
    val state: String,
    val clusterId: String?,
    val allowedToLeaveCluster: Boolean
)

@Serializable
data class DiskInfo(
    val diskId: String,
    val mountPoint: String,
    val hostId: String,
    val state: String,
    val deviceNodeName: String,
    val diskTagList: MutableList<String>,
    val diskSerialNumber: String
)