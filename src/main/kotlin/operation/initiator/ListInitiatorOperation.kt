package operation.initiator

import operation.AuthedHttpOperation
import operation.request.Requester
import utils.Initiator

@kotlinx.serialization.Serializable
class ListInitiatorReq


@kotlinx.serialization.Serializable
class ListInitiatorResp(
    val data: List<Initiator>
)

class ListInitiatorOperation: AuthedHttpOperation<ListInitiatorReq, ListInitiatorResp>(
    method = Requester.Method.GET,
    path = "v1/initiators",
) {
    override suspend fun invoke(input: ListInitiatorReq): ListInitiatorResp = getRequester().send(input).parse()
}

