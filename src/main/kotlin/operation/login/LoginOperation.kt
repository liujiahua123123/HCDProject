package operation.login

import operation.HttpOperation
import operation.request.Requester

@kotlinx.serialization.Serializable
data class LoginReq(
    val grant_type: String = "password",
    val password: String  = "Hello123!",
    val username: String = "admin",
    val int: Int = 0
)

@kotlinx.serialization.Serializable
data class LoginResp(val access_token: String, val expire: Long)


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