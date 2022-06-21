package operation.cluster

import operation.AuthedHttpOperation
import operation.request.Requester

@kotlinx.serialization.Serializable
data class CreateClusterInfo(
    val clusterName: String,
    val minClusterSize: Int,
    val replicationFactor: Int,
    val virtualIp: String,
)

@kotlinx.serialization.Serializable
data class CreateClusterReq(
    val cluster: CreateClusterInfo,
    val hosts: List<String>
)



@kotlinx.serialization.Serializable
data class CreateClusterResp(
    val taskId: String
)

class CreateClusterOperation: AuthedHttpOperation<CreateClusterReq, CreateClusterResp>(
    path = "/v1/clusters/task",
    method = Requester.Method.POST
) {
    override suspend fun invoke(input: CreateClusterReq): CreateClusterResp = getRequester().send(input).parse()
}

