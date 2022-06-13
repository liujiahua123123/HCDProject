package operation

import operation.login.LoginOperation
import operation.login.LoginReq
import request.pipeline.AsyncLogger
import request.pipeline.StatusChecker
import operation.request.Requester
import request.pipeline.KeyExchangePipeline
import kotlin.math.log

interface Operation<I, O> {
    suspend operator fun invoke(input: I): O
}


abstract class HttpOperation<I, O>(
    val path: String,
    val method: Requester.Method
) : Operation<I, O> {

    var domain: String = ""

    var loggerName = this.javaClass.simpleName.removeSuffix("Operation")

    open fun getRequester(): Requester {
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

abstract class AuthedHttpOperation<I, O>(
   path: String, method: Requester.Method
): HttpOperation<I, O>(path,method){
    override fun getRequester(): Requester {
        return super.getRequester().apply {
            addPipeline(KeyExchangePipeline)
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