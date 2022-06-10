package server.trace

import utils.createUuid4

/**
 * Traceable is a kind of respond that server generated for some request
 *
 * This means that the request take time and the server is handling it, front end should
 * check for complete with a schedule task
 */
interface Traceable<T:Any> {
    val id: String


    enum class State{
        SCHEDULING,
        COMPUTED,
        THROWN
    }

    val state:State

    abstract fun getResult():T
    abstract fun getFailureReason():Throwable

    abstract fun toTracingData(): TracingData
}

@kotlinx.serialization.Serializable
data class TracingData(
    val traceId: String,
    val currStep: Int,
    val stepNames: List<String>
)