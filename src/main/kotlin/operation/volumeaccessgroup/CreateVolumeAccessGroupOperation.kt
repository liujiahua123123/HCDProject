package operation.volumeaccessgroup

import operation.AuthedHttpOperation
import operation.request.Requester
import utils.IdObject


@kotlinx.serialization.Serializable
data class CreateVolumeAccessGroupReq(
    val volumeAccessGroupName: String,
    val clusterId: String,
    val initiators: List<IdObject> = emptyList(),
    val volumes: List<IdObject> = emptyList(),
)

@kotlinx.serialization.Serializable
class CreateVolumeAccessGroupResp

class CreateVolumeAccessGroupOperation :AuthedHttpOperation<CreateVolumeAccessGroupReq,CreateVolumeAccessGroupResp>(
    method = Requester.Method.POST,
    path = "/v1/volume-access-groups",
) {
    override suspend fun invoke(input: CreateVolumeAccessGroupReq): CreateVolumeAccessGroupResp = getRequester().send(input).parse()
}