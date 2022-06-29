package operation.volumeaccessgroup

import operation.AuthedHttpOperation
import operation.request.Requester


@kotlinx.serialization.Serializable
data class ListVolumeAccessGroupReq(
    @kotlinx.serialization.Transient val clusterId: String = ""
)

@kotlinx.serialization.Serializable
class ListVolumeAccessGroupResp()


class ListVolumeAccessGroupOperation: AuthedHttpOperation<ListVolumeAccessGroupReq, ListVolumeAccessGroupResp>(
    method = Requester.Method.GET,
    path = "/v1/volume-access-groups"
) {
    override suspend fun invoke(input: ListVolumeAccessGroupReq): ListVolumeAccessGroupResp = getRequester().apply {
        addPathParameter(input.clusterId)
    }.send(input).parse()
}

