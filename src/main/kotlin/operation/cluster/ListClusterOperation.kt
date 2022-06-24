package operation.cluster

import operation.AuthedHttpOperation
import operation.request.Requester
import utils.ClusterInfo


@kotlinx.serialization.Serializable
class ListClusterReq()

@kotlinx.serialization.Serializable
data class ListClusterResp(
    val data: List<ClusterInfo>
)

class ListClusterOperation: AuthedHttpOperation<ListClusterReq, ListClusterResp>(
    method = Requester.Method.GET,
    path = "/v1/clusters"
) {
    override suspend fun invoke(input: ListClusterReq): ListClusterResp = getRequester().send(input).parse()
}