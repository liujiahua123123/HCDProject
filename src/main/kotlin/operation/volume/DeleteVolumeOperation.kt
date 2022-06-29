package operation.volume

import kotlinx.serialization.Transient
import operation.AuthedHttpOperation
import operation.request.Requester

@kotlinx.serialization.Serializable
data class DeleteVolumeReq(
    @Transient val volumeId: String = "",
    @Transient val clusterId: String = ""
)

@kotlinx.serialization.Serializable
class DeleteVolumeResp()


class DeleteVolumeOperation:AuthedHttpOperation<DeleteVolumeReq,DeleteVolumeResp>(
    method = Requester.Method.DELETE,
    path = "/volumes/",
) {
    override suspend fun invoke(input: DeleteVolumeReq): DeleteVolumeResp {
        return getRequester().apply {
            addPathParameter(input.clusterId)
            addPathParameter(input.volumeId)
        }.send(input).parse()
    }
}