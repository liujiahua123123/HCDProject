package operation.cluster

import kotlinx.serialization.Transient
import operation.AuthedHttpOperation
import operation.request.Requester

@kotlinx.serialization.Serializable
data class DeleteClusterReq(
    @Transient val clusterId: String = ""
)

@kotlinx.serialization.Serializable
data class DeleteClusterResp(
    val taskId: String
)

class DeleteClusterOperation: AuthedHttpOperation<DeleteClusterReq, DeleteClusterResp>(
    path = "/v1/clusters/task/",
    method = Requester.Method.DELETE
) {
    override suspend fun invoke(input: DeleteClusterReq): DeleteClusterResp {
        return getRequester().apply {
            addPathParameter(input.clusterId)
        }.send(input).parse()
    }
}