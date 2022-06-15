package operation.host
import kotlinx.serialization.Serializable
import operation.AuthedHttpOperation
import operation.request.Requester
import utils.HostInfo
import utils.KeyExchangeService

@Serializable
data class ListHostReq(
    val clusterId: String? = null,
    var onlyFreeHosts: Boolean = false,
)

@Serializable
data class ListHostResp(
    val data: List<HostInfo>
)

class ListHostOperation: AuthedHttpOperation<ListHostReq,ListHostResp>(
    method = Requester.Method.GET,
    path = "/v1/hosts"
){
    override suspend fun invoke(input: ListHostReq): ListHostResp {
        return getRequester().send(input).parse()
    }
}

