package utils

import kotlinx.html.currentTimeMillis
import operation.login.LoginOperation
import operation.login.LoginReq
import operation.login.LoginResp
import java.util.concurrent.ConcurrentHashMap


data class AccessKey(
    val content: String,
    val expire: Long
)

object KeyExchangeService{
    private val keyManagement = ConcurrentHashMap<String, AccessKey>()
    private val credentialManagement = ConcurrentHashMap<String, Pair<String, String>>()

    fun register(host: String, username: String = "admin", password: String = "Hello123!", resp: LoginResp? = null){
        credentialManagement[host] = Pair(username, password)
        if(resp != null){
            keyManagement[host] = AccessKey(resp.access_token,resp.expire * 1000 + currentTimeMillis())
        }
    }

    suspend fun get(host: String):AccessKey{
        if(!credentialManagement.containsKey(host)){
            error("$host didn't register for KeyExchangeService")
        }
        val key = keyManagement[host] ?: return renewKey(host)
        if(key.expire < currentTimeMillis()){
            return renewKey(host)
        }
        return key
    }

    private suspend fun renewKey(host: String):AccessKey{
        val resp =
            LoginOperation()
                .apply {
                    domain = host
                    loggerName = "TokenRenew"
                }.invoke(
                    with(credentialManagement[host]!!) {
                        LoginReq(
                            username = this.first,
                            password = this.second
                        )
                    }
                )

        return AccessKey(resp.access_token,resp.expire * 1000 + currentTimeMillis()).also {
            keyManagement[host] = it
        }
    }
}