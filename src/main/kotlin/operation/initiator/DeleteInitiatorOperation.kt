package operation.initiator

import kotlinx.serialization.Transient
import operation.AuthedHttpOperation
import operation.request.Requester

@kotlinx.serialization.Serializable
data class DeleteInitiatorReq(
    @Transient val initiatorId: String = ""
)

@kotlinx.serialization.Serializable
class DeleteInitiatorResp

class DeleteInitiatorOperation: AuthedHttpOperation<DeleteInitiatorReq, DeleteInitiatorResp>(
    method = Requester.Method.DELETE,
    path = "/v1/initiators",
) {
    override suspend fun invoke(input: DeleteInitiatorReq): DeleteInitiatorResp = getRequester().apply {
        addPathParameter(input.initiatorId)
    }.send(input).parse()
}