package operation

import operation.pipeline.AsyncLogger
import operation.pipeline.StatusChecker
import operation.request.Requester

interface Operation<I,O>{
    suspend operator fun invoke(input: I): O
}


abstract class HttpOperation<I,O>(
    private val path: String,
    private val method: Requester.Method
): Operation<I, O> {

    var domain: String = ""

    fun getRequester(): Requester {
        if(domain.isEmpty()){
            error("HttpOperation need a specific domain")
        }
        return Requester().also {
            it.addPathParameter(path)
            it.domain = domain
            it.method = method
            it.addPipeline(StatusChecker)
            it.addPipeline(AsyncLogger())
        }
    }
}

@kotlinx.serialization.Serializable
data class LoginReq(
    val grant_type: String = "password",
    val password: String  = "Hello123!",
    val username: String = "admin",
    val int: Int = 0
)

@kotlinx.serialization.Serializable
data class LoginResp(val access_token: String)


class LoginOperation: HttpOperation<LoginReq, LoginResp>(
    method = Requester.Method.FORM_POST,
    path = "/oauth/token"
){
    override suspend fun invoke(input: LoginReq): LoginResp {
        return getRequester().apply {
            addHeader("Authorization","Basic aGNkLWNsaWVudDpoY2Qtc2VjcmV0")
        }.send(input).parse()
    }
}

suspend fun main(){
    val op =  LoginOperation()

    op.domain = "172.16.4.248:8443"
    println(op(LoginReq(
        username = "a"
    )))
}