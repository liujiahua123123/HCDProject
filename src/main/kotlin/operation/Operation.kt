package operation

import io.ktor.http.*
import kotlinx.html.I
import operation.login.LoginOperation
import operation.login.LoginReq
import request.pipeline.AsyncLogger
import request.pipeline.StatusChecker
import operation.request.Requester
import request.pipeline.KeyExchangePipeline
import kotlin.reflect.full.createInstance

interface Operation<I, O> {
    suspend operator fun invoke(input: I): O
}


abstract class HttpOperation<I, O>(
    val path: String,
    val method: Requester.Method
) : Operation<I, O> {

    var portal: String = ""

    var loggerName = this.javaClass.simpleName.removeSuffix("Operation")

    open fun getRequester(): Requester {
        if (portal.isEmpty()) {
            error("HttpOperation need a specific domain")
        }
        return Requester().also {
            it.addPathParameter(path)
            it.portal = portal
            it.method = method
            it.addPipeline(StatusChecker)
            it.addPipeline(AsyncLogger(loggerName))
        }
    }
}

suspend fun <T> httpOperationScope(portal: String, builder: suspend HttpOperationBuilder.() -> T): T{
    return builder(HttpOperationBuilder(portal))
}

class HttpOperationBuilder(
    val portal: String
){
    inline fun <reified T:HttpOperation<*,*>> create(): T{
        val t = T::class.createInstance()
        t.portal = portal
        return t
    }

    suspend inline fun <I,O,reified T:HttpOperation<I,O>> invoke (input:I):O{
        val t = T::class.createInstance()
        t.portal = portal
        return t.invoke(input)
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

    op.portal = "172.16.4.248:8443"
    println(
        op(
            LoginReq(
                username = "a"
            )
        )
    )
}