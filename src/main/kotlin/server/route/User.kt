package server.route

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import operation.login.LoginOperation
import operation.login.LoginReq
import operation.login.LoginResp
import server.*
import utils.*

fun Routing.userRoute(){
    get("/login"){
        call.respondFile(STATIC_FILES.findFile("Login.html"))
    }

    handleDataPost("/login"){
        val credential = call.readDataRequest<LoginRequest>()
        val user = UserManager.login(credential.username,credential.password)?: userInputError("Username/Password mismatched")
        call.bindUser(user)
        call.respondOK()
    }

    handleDataPost("/register"){
        val credential = call.readDataRequest<LoginRequest>()
        val user = UserManager.addUser(credential.username,credential.password)?: userInputError("Username already exists")
        call.bindUser(user)
        call.respondOK()
    }

    get("/"){
        ifLogin {
            call.respondFile(STATIC_FILES.findFile("Home.html"))
        }
    }

    handleDataPost("/portal/history"){
        ifLogin {user ->
            call.respondOK(user.getAllData<ConnectionHistory>())
        }
    }

    handleDataPost("/portal/connect"){
        ifLogin {user ->
            val data = call.readDataRequest<ConnectPortalRequest>()

            call.respondTraceable(OperationExecutor.addExecutorTask<LoginResp>(2){
                this.currStepName = "Connecting"
                this.currStep++
                this.currStepName = "Exchanging Token"
                val resp = LoginOperation().apply {
                    this.portal = data.portal
                }.invoke(
                    LoginReq(
                        username = data.username,
                        password = data.password
                    )
                )

                user.dataScope<ConnectionHistory> {
                    it.removeIf { ele -> ele.portal == data.portal}
                    it.add(0,ConnectionHistory(
                        data.portal,data.username,data.password
                    ))
                    true
                }

                KeyExchangeService.register(data.portal,data.username,data.password,resp)
                resp
            })
        }
    }

}

@kotlinx.serialization.Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@kotlinx.serialization.Serializable
data class ConnectPortalRequest(
    val username: String,
    val password: String,
    val portal: String
)


