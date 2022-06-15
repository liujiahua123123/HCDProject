package operation.disk

import operation.AuthedHttpOperation
import operation.request.Requester

@kotlinx.serialization.Serializable
data class DiskRemoveTagReq(
    val hostId: String,
    val diskIds: List<String>,
    val diskTag: String
)

@kotlinx.serialization.Serializable
class DiskRemoveTagResp(
    val taskId: String
)

class DiskRemoveTagOperation: AuthedHttpOperation<DiskRemoveTagReq, DiskRemoveTagResp>(
    method = Requester.Method.POST,
    path = "/v1/disks/tag/auto-disable"
){
    override suspend fun invoke(input: DiskRemoveTagReq): DiskRemoveTagResp = getRequester().send(input).parse()
}