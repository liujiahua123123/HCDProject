package operation

import operation.login.LoginOperation
import operation.login.LoginReq
import request.pipeline.AsyncLogger
import request.pipeline.StatusChecker
import operation.request.Requester
import kotlin.math.log

interface Operation<I, O> {
    suspend operator fun invoke(input: I): O
}


abstract class HttpOperation<I, O>(
    private val path: String,
    private val method: Requester.Method
) : Operation<I, O> {

    var domain: String = ""

    var loggerName = this.javaClass.simpleName.removeSuffix("Operation")

    fun getRequester(): Requester {
        if (domain.isEmpty()) {
            error("HttpOperation need a specific domain")
        }
        return Requester().also {
            it.addPathParameter(path)
            it.domain = domain
            it.method = method
            it.addPipeline(StatusChecker)
            it.addPipeline(AsyncLogger(loggerName))
        }
    }
}

suspend fun main() {
    val op = LoginOperation()

    op.domain = "172.16.4.248:8443"
    println(
        op(
            LoginReq(
                username = "a"
            )
        )
    )
}