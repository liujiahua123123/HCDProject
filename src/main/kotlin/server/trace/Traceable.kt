package server.trace

/**
 * Traceable is a kind of respond that server generated for some request, similar to Future
 *
 * This means that the request take time and the server is handling it, front end should
 * check for complete with a schedule task
 */
interface Traceable {
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
    fun getResponse():String
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