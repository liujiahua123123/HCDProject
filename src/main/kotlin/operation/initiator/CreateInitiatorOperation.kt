package operation.initiator

import operation.AuthedHttpOperation
import operation.request.Requester
import utils.createUuid4

@kotlinx.serialization.Serializable
data class CreateInitiatorReq(
    //val initiatorId: String = createUuid4(),
    val initiatorName: String,
    val iqn: String
)

@kotlinx.serialization.Serializable
class CreateInitiatorResp


class CreateInitiatorOperation: AuthedHttpOperation<CreateInitiatorReq, CreateInitiatorResp>(
    path = "/v1/initiators",
    method = Requester.Method.POST,
) {
    override suspend fun invoke(input: CreateInitiatorReq): CreateInitiatorResp = getRequester().send(input).parse()
}