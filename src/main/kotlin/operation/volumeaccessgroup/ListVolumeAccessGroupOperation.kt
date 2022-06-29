package operation.volumeaccessgroup

import operation.AuthedHttpOperation
import operation.request.Requester
import utils.KeyExchangeService


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


suspend fun main(){
    KeyExchangeService.register("172.16.4.248:8443", "admin", "Hello123")
    ListVolumeAccessGroupOperation().apply {
        portal = "172.16.4.248:8443"
    }.invoke(ListVolumeAccessGroupReq(
        clusterId = "5546fad7-9839-4989-a006-7edfdfcc0fde"
    ))
}
