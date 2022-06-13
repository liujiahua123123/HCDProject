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