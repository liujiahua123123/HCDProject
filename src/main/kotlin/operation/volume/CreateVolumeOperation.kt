package operation.volume

import operation.AuthedHttpOperation
import operation.request.Requester

@kotlinx.serialization.Serializable
data class CreateVolumeReq(
    val volumeName: String,
    val clusterId: String,
    val volumeSize: Long,
    val blockSize: Long,
    val type: String,
    val compressionAlgorithm: String,
    val enableDedup: Boolean,
    /**
     compressionAlgorithm": "NONE",
     enableDedup": true,
     */
)

@kotlinx.serialization.Serializable
data class CreateVolumeResp(
    val taskId: String
)

class CreateVolumeOperation: AuthedHttpOperation<CreateVolumeReq, CreateVolumeResp>(
    method = Requester.Method.POST,
    path = "/v1/volumes/task/create"
) {
    override suspend fun invoke(input: CreateVolumeReq): CreateVolumeResp = getRequester().send(input).parse()
}