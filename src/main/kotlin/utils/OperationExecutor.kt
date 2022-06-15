package utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import operation.Operation
import server.ServerJson
import server.ServerResponse
import server.trace.Traceable
import server.trace.TracingData
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext


object OperationExecutor {

    val scope = CoroutineScope(kotlinx.coroutines.Job())
    val map = ConcurrentHashMap<String, Job<*>>()

    suspend inline fun <reified T : Any> addExecutorTask(numOfStep: Int, crossinline workflow: suspend Job<T>.() -> T):Traceable {
        val job = Job<T>(numOfStep)
        map[job.id] = job
        scope.launch {
            val result = kotlin.runCatching {
                workflow.invoke(job)
            }
            result.fold(onFailure = {
                job.state = Traceable.State.THROWN
                job.exceptionHolder = it
            }, onSuccess = {
                println(it)
                job.state = Traceable.State.COMPUTED
                /** Type Erase */
                job.resultHolder = ServerJson.encodeToString(ServerResponse(
                    success = true, errorMessage = "",isTracingTask = false, data = it
                ))
            })
        }
        return job
    }


    fun get(id: String):Job<*>?{
        return map[id].apply {
            if(this != null && this.state != Traceable.State.SCHEDULING){
                map.remove(id)
            }
        }
    }
}


class Job<T : Any>(var totalStep: Int) : Traceable{
    override val id: String = createUuid4()

    override var state: Traceable.State = Traceable.State.SCHEDULING

    var resultHolder: String? = null
    var exceptionHolder: Throwable? = null


    var currStep: Int = 1
    var currStepName: String = "initiating"

    fun updateProgress(currStep: Int, totalStep: Int, name: String){
        this.currStep = currStep
        this.totalStep = totalStep
        this.currStepName = name
    }

    fun updateProgress(name: String) = updateProgress(currStep + 1, totalStep, name)

    override fun getResponse(): String {
        return resultHolder!!
    }

    override fun getFailureReason(): Throwable {
        return exceptionHolder!!
    }

    override fun toTracingData(): TracingData {
        return TracingData(
            this.id, this.totalStep, this.currStep, this.currStepName
        )
    }

}