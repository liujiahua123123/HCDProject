package utils

import io.ktor.server.application.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import server.userInputError


data class User(
    val username: String,
    val password: String,
    val uuid: String = createUuid4()
)

fun ApplicationCall.bindUser(user: User){

}


object UserManager {
    private val file = DATA_FILES.findFile("user.json")
    private val lock = Mutex()
    private val runtimeId = createUuid4()

    suspend fun addUser(username: String, password: String){
        lock.withLock {
            val list = file.deserializeList<User>()
            list.removeAll { it.username == username }
            list.add(User(username,password))
            file.writeList(list)
        }
    }

    fun login(username: String, password: String):User?{
        val list = file.deserializeList<User>()
        return list.firstOrNull { it.username == username && it.password == password }
    }


}