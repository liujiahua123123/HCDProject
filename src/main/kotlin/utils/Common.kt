package utils

import kotlinx.serialization.Serializable
import operation.cluster.CreateClusterInfo
import java.util.StringJoiner


@Serializable
data class ClusterInfo(
    val clusterId: String,
    val clusterName: String,
    val replicationFactor: Int,
    val minClusterSize: Int,
    val virtualIp: String
)

@Serializable
data class HostInfo(
    val hostId: String,
    val hostName: String,
    val state: String,
    val clusterId: String?,
    val allowedToLeaveCluster: Boolean,
    val managementAddress: String?
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

@Serializable
data class VolumeInfo(
    val volumeId: String,
    val volumeNumericalId: Int,
    val volumeName: String,
    val clusterId: String,
    val volumeSize: Long,
    val blockSize: Long,
    val accessLevel: String,
    val status: String,
    val compressionAlgorithm: String,
    val iscsiTarget: IscsiTarget
)

@Serializable
data class IscsiTarget(
    val ip: String,
    val port: Int,
    val iqn: String
)

@Serializable
data class Initiator(
    val initiatorId: String,
    val initiatorName: String,
    val iqn: String
)


