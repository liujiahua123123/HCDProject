package operation.host

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import operation.AuthedHttpOperation
import operation.request.Requester
import utils.DiskInfo
import utils.KeyExchangeService

@Serializable
data class ListDiskByHostReq(
    val hostId: String
)

@Serializable
data class ListDiskByHostResp(
    val data: List<DiskInfo>
)


class ListDiskByHostOperation : AuthedHttpOperation<ListDiskByHostReq, ListDiskByHostResp>(
    path = "/v1/disks/by-host/",
    method = Requester.Method.GET
) {
    override suspend fun invoke(input: ListDiskByHostReq): ListDiskByHostResp {
        return getRequester()
            .apply {
                addPathParameter(input.hostId)
            }
            .send(input).parse()
    }
}

suspend fun main() {
    KeyExchangeService.register("172.16.4.248:8443")
    println(ListHostOperation().apply { portal = "172.16.4.248:8443" }(ListHostReq()))
    println(
        ListDiskByHostOperation().apply { portal = "172.16.4.248:8443" }(
            ListDiskByHostReq(
                hostId = "00000000-0000-0000-0000-0CC47AD453B0"
            )
        )
    )
}

