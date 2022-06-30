package operation.initiator

import operation.AuthedHttpOperation
import operation.request.Requester

@kotlinx.serialization.Serializable
data class CreateInitiatorReq(
    val initiatorName: String,
    val iqn: String
)

@kotlinx.serialization.Serializable
class CreateInitiatorResp()


class CreateInitiatorOperation: AuthedHttpOperation<CreateInitiatorReq, CreateInitiatorResp>(
    path = "/v1/initiators",
    method = Requester.Method.PUT
) {
    override suspend fun invoke(input: CreateInitiatorReq): CreateInitiatorResp {
        TODO("Not yet implemented")
    }
}