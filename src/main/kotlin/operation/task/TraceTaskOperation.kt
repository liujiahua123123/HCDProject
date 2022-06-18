package operation.task

import kotlinx.serialization.Transient
import operation.AuthedHttpOperation
import operation.request.Requester

@kotlinx.serialization.Serializable
data class TraceTaskReq(
    @Transient val taskId: String = ""
)

@kotlinx.serialization.Serializable
data class TraceTaskResp(
    val progress: Int,
    val taskName: String,
    val statusName: String? = null,
    val resourceId: String? = null,
)

class TraceTaskOperation: AuthedHttpOperation<TraceTaskReq, TraceTaskResp>(
    path = "/v1/tasks/",
    method = Requester.Method.GET
) {
    override suspend fun invoke(input: TraceTaskReq): TraceTaskResp {
        return getRequester().apply {
            this.addPathParameter(input.taskId)
        }.send(input).parse()
    }
}