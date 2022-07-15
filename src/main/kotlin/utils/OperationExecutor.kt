package utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import server.ServerResponse
import server.trace.ResultHolder
import server.trace.Traceable
import server.trace.TracingData
import java.util.concurrent.ConcurrentHashMap

object OperationExecutor {
    private val scope = CoroutineScope(kotlinx.coroutines.Job())

    private val map = ConcurrentHashMap<String, Job<*>>()

    inline fun <reified T : Any> serverResponseSerializer(): KSerializer<ServerResponse<T>> = serializer()

    suspend inline fun <reified T : Any> addExecutorTask(
        noinline workflow: suspend Job<T>.() -> T
    ): Traceable<T> = addExecutorTask(serverResponseSerializer(), workflow)

    suspend fun <T : Any> addExecutorTask(
        serializer: KSerializer<ServerResponse<T>>,
        workflow: suspend Job<T>.() -> T
    ): Traceable<T> {
        val job = Job(0, ResultHolder(null, serializer))
        map[job.id] = job
        scope.launch {
            val result = kotlin.runCatching {
                workflow.invoke(job)
            }
            result.fold(onFailure = {
                job.exceptionHolder = it
                job.state = Traceable.State.THROWN
            }, onSuccess = {
                job.holder.result = it
                job.state = Traceable.State.COMPUTED
            })
        }
        return job
    }


    fun get(id: String): Job<*>? {
        return map[id].apply {
            if (this != null && this.state != Traceable.State.SCHEDULING) {
                map.remove(id)
            }
        }
    }
}


class Job<T>(var totalStep: Int, val holder: ResultHolder<T>) : Traceable<T> {
    override val id: String = createUuid4()

    override var state: Traceable.State = Traceable.State.SCHEDULING

    var exceptionHolder: Throwable? = null

    private var currStep: Int = 0
    private var currStepName: String = "initiating"
    private val mutex = Mutex()

    suspend fun updateProgress(currStep: Int, totalStep: Int, name: String) {
        mutex.withLock {
            this.currStep = currStep
            this.totalStep = totalStep
            this.currStepName = name
        }
    }

    suspend fun updateProgressDescription(name: String) {
        mutex.withLock {
            this.currStepName = name
        }
    }

    suspend fun updateProgress(name: String){
        mutex.withLock {
            this.currStep = this.currStep + 1
            this.currStepName = name
        }
    }

    override fun getResult(): ResultHolder<T> {
        return holder
    }

    override fun getFailureReason(): Throwable {
        return exceptionHolder ?: error("Job Failure is unavailable")
    }

    override fun toTracingData(): TracingData {
        return TracingData(
            this.id, this.totalStep, this.currStep, this.currStepName
        )
    }
}