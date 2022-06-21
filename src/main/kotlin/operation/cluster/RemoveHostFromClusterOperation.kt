package operation.cluster

import operation.AuthedHttpOperation
import operation.request.Requester

@kotlinx.serialization.Serializable
data class RemoveHostFromClusterReq(
    val clusterId: String,
    val hostIpAddress: String,
    val hostId: String,
    val force: Boolean = true
)

@kotlinx.serialization.Serializable
data class RemoveHostFromClusterResp(
    val taskId: String
)

class RemoveHostFromClusterOperation:AuthedHttpOperation<RemoveHostFromClusterReq,RemoveHostFromClusterResp>(
    path = "/v1/clusters/expansion/remove-host-task",
    method = Requester.Method.POST
){
    override suspend fun invoke(input: RemoveHostFromClusterReq): RemoveHostFromClusterResp = getRequester().send(input).parse()
}