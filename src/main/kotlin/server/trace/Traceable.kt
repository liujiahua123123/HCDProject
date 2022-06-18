package server.trace

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import server.ServerResponse
import utils.Job

/**
 * Traceable is a kind of respond that server generated for some request, similar to Future
 *
 * This means that the request take time and the server is handling it, front end should
 * check for complete with a schedule task
 */

/**
 * Traceable should hold {result} along with ServerResponse<T>'s serializer for encoding response
 */
data class ResultHolder<T>(
    var result: T?,
    val serialization: KSerializer<ServerResponse<T>>
)

interface Traceable<T> {
    val id: String

    enum class State{
        SCHEDULING,
        COMPUTED,
        THROWN
    }

    val state:State

    /**
     * Serialized Result
     */
    fun getResult(): ResultHolder<T>
    fun getFailureReason():Throwable

    fun toTracingData(): TracingData
}

@kotlinx.serialization.Serializable
data class TracingData(
    val traceId: String,
    val totalStep: Int,
    val currStep: Int,
    val currStepName: String
)