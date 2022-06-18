package operation.disk

import kotlinx.serialization.Transient
import operation.AuthedHttpOperation
import operation.request.Requester

@kotlinx.serialization.Serializable
data class DiskRemoveTagReq(
    @Transient val hostId: String = "",
    @Transient val diskIds: List<String> = emptyList(),
    @Transient val diskTag: String = ""
)

@kotlinx.serialization.Serializable
class DiskRemoveTagResp(
    val taskId: String
)

class DiskRemoveTagOperation: AuthedHttpOperation<DiskRemoveTagReq, DiskRemoveTagResp>(
    method = Requester.Method.DELETE,
    path = "/v1/disks/tag/auto-disable"
){
    override suspend fun invoke(input: DiskRemoveTagReq): DiskRemoveTagResp{
        return getRequester().apply {
            addPathParameter(input.hostId)
            addPathParameter(input.diskIds.joinToString(","))
            addPathParameter(input.diskTag)
        }.send(input).parse()
    }
}