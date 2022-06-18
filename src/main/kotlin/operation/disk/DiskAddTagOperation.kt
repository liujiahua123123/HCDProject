package operation.disk

import operation.AuthedHttpOperation
import operation.request.Requester


@kotlinx.serialization.Serializable
data class DiskAddTagReq(
    val hostId: String,
    val diskIds: List<String>,
    val diskTag: String
)

@kotlinx.serialization.Serializable
class DiskAddTagResp(
    val taskId: String
)

class DiskAddTagOperation:AuthedHttpOperation<DiskAddTagReq,DiskAddTagResp>(
    method = Requester.Method.POST,
    path = "/v1/disks/tag/auto-enable"
){
    override suspend fun invoke(input: DiskAddTagReq): DiskAddTagResp = getRequester().send(input).parse()
}