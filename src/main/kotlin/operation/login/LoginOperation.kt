package operation.login

import kotlinx.serialization.SerialName
import operation.HttpOperation
import operation.request.Requester
import kotlinx.serialization.Serializable

@Serializable
data class LoginReq(
    val grant_type: String = "password",
    val password: String = "Hello123!",
    val username: String = "admin",
    val int: Int = 0
)

@Serializable
data class LoginResp(
    val access_token: String,
    @SerialName("expires_in") val expire: Long
)


class LoginOperation : HttpOperation<LoginReq, LoginResp>(
    method = Requester.Method.FORM_POST,
    path = "/oauth/token"
) {
    override suspend fun invoke(input: LoginReq): LoginResp {
        return getRequester().apply {
            addHeader("Authorization", "Basic aGNkLWNsaWVudDpoY2Qtc2VjcmV0")
        }.send(input).parse()
    }
}