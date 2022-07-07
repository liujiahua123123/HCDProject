package operation.volumeaccessgroup

import operation.AuthedHttpOperation
import operation.request.Requester


@kotlinx.serialization.Serializable
data class DeleteVolumeAccessGroupReq(
    val volumeAccessGroupId: String,
    val clusterId: String
)

@kotlinx.serialization.Serializable
class DeleteVolumeAccessGroupResp


class DeleteVolumeAccessGroupOperation: AuthedHttpOperation<DeleteVolumeAccessGroupReq, DeleteVolumeAccessGroupResp>(
    method = Requester.Method.DELETE,
    path = "/v1/volume-access-groups/",
)
{
    override suspend fun invoke(input: DeleteVolumeAccessGroupReq): DeleteVolumeAccessGroupResp = getRequester().apply {
        addPathParameter(input.clusterId)
        addPathParameter(input.volumeAccessGroupId)
    }.send(input).parse()
}