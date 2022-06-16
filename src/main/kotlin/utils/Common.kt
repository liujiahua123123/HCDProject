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
    val diskNumericalId: Int,
    val hostId: String,
    val mountPoint: String,
    val state: String,
    val physicalState: String,
    val deviceNodeName: String,
    val diskTagList: MutableList<String>,
    val diskSerialNumber: String,
    val diskSize: Long,
    val diskUsage: Long,
    val diskFreeSpace: Long,
    val diskUsableSize: Long,
    val downstreamDevice: String,
    val pciGroupUpstreamBus: String,
    val carrierIndex: String,
    val inVolumeGroup: Boolean,
)