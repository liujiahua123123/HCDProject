package utils

import io.ktor.server.application.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.html.currentTimeMillis


interface User {
    val username: String
    val password: String
    val uuid: String
}

@kotlinx.serialization.Serializable
data class SavedUser(
    override val username: String,
    override val password: String,
    override val uuid: String = createUuid4()
) : User


class UserFromLoginToken(
    override val username: String,
    override val uuid: String
) : User {
    override val password: String
        get() = error("Can not retrieve user password under curr context")
}


fun ApplicationCall.bindUser(user: User) {
    this.response.cookies.append(
        UserManager.COOKIE_TOKEN, UserManager.createLoginToken(user)
    )
    this.response.cookies.append(
        UserManager.COOKIE_USERNAME, user.username
    )
    this.response.cookies.append(
        UserManager.COOKIE_UID, user.uuid
    )
}


object UserManager {
    private val file = DATA_FILES.findFile("user.json")
    private val lock = Mutex()

    private val salt = createUuid4()
    private const val ACCESS_KEY_VALID = 1000 * 60 * 60 * 24 * 7;//7 days

    const val COOKIE_TOKEN = "_user_token"
    const val COOKIE_USERNAME = "_user"
    const val COOKIE_UID = "_uid"

    suspend fun addUser(username: String, password: String):User?{
        val user = SavedUser(username, password)
        lock.withLock {
            val list = file.deserializeList<SavedUser>()
            if(list.any{ it.username == username }){
                return null
            }
            list.add(user)
            file.writeList(list)
        }
        return user
    }

    fun login(username: String, password: String): User? {
        val list = file.deserializeList<SavedUser>()
        return list.firstOrNull { it.username == username && it.password == password }
    }

    fun createLoginToken(user: User): String {
        val data = user.username + "#" + (currentTimeMillis() + ACCESS_KEY_VALID) + "#" + user.uuid + "#"
        return data + (data + salt).md5()
    }

    fun verifyLoginToken(key: String): User? {
        if (key.length > 2048) {
            return null
        }
        val data = key.split("#")
        if (data.size != 4) {
            return null
        }

        try {
            if (data[1].toLong() <= currentTimeMillis()) {
                return null
            }
        } catch (e: Exception) {
            return null
        }
        if ((data[0] + "#" + data[1] + "#" + data[2] + "#" + salt).md5() != data[3]) {
            return null
        }
        return UserFromLoginToken(data[0], data[2])
    }

}


@kotlinx.serialization.Serializable
sealed class UserData(
    val id:String = createUuid4(),
)

@kotlinx.serialization.Serializable
data class ConnectionHistory(
    val portal: String,
    val username: String,
    val password: String
): UserData()

object UserDataManagement {
    val lock = Mutex()
    val folder = DATA_FILES.findSub("userdata")

    fun readAll(uid: String): MutableList<UserData> {
        return folder.findFile("$uid.json").deserializeList<UserData>()
    }

    inline fun <reified T : UserData> getAll(uid: String): MutableList<T> {
        return readAll(uid).filterIsInstanceTo(mutableListOf())
    }

    inline fun <reified T : UserData> first(uid: String): T? {
        return readAll(uid).firstOrNull{ it is T } as T?
    }

    suspend fun save(uid: String, userData: UserData){
        lock.withLock {
            folder.findFile("$uid.json").writeList(readAll(uid).apply {
                removeIf { it.id == userData.id }
                add(0, userData)
            })
        }
    }

    suspend fun delete(uid: String, dataId: String){
        lock.withLock {
            folder.findFile("$uid.json").writeList(this.readAll(uid).apply{removeIf { it.id == dataId }})
        }
    }

    suspend inline fun <reified T:UserData> scope(uid: String, block: (MutableList<T>) -> Boolean){
        val all = readAll(uid)
        val selected = getAll<T>(uid)
        selected.forEach {
            all.remove(it)
        }
        if(block(selected)){
            all.addAll(0,selected)
            lock.withLock {
                folder.findFile("$uid.json").writeList(all)
            }
        }
    }

}



inline fun <reified T : UserData> User.getAllData(): MutableList<T> = UserDataManagement.getAll(uuid)
suspend fun User.saveData(data: UserData) = UserDataManagement.save(uuid,data)

suspend inline fun <reified T:UserData> User.dataScope(block: (MutableList<T>) -> Boolean) = UserDataManagement.scope(uuid,block)