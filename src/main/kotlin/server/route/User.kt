package server.route

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import utils.STATIC_FILES
import server.handleDataPost
import server.readDataRequest
import server.respondOK
import server.userInputError
import utils.UserManager
import utils.bindUser

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

}

@kotlinx.serialization.Serializable
data class LoginRequest(
    val username: String,
    val password: String
)