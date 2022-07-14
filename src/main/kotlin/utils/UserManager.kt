package utils

import io.ktor.server.application.*
import io.ktor.util.collections.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.html.currentTimeMillis
import operation.cluster.CreateClusterInfo
import operation.cluster.CreateClusterReq
import server.route.HostWithDisks
import java.util.Collections


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
    private val file = DATA_FILES.findFile("user.yaml")
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


@kotlinx.serialization.Serializable
data class ClusterTemplate(
    val templateName: String,
    val portal: String,
    val creator: CreateClusterInfo,
    val hosts: Map<String, HostTemplate>,
    val initiators: List<InitiatorTemplate>,
    val volumes: List<VolumeTemplate>,
    val volumeAccessGroups: List<VolumeAccessGroupTemplate>
): UserData()

@kotlinx.serialization.Serializable
data class HostTemplate(
    val disks: Map<String, DiskTemplate>
)

@kotlinx.serialization.Serializable
data class InitiatorTemplate(
    val name: String,
    val iqn: String,
)

@kotlinx.serialization.Serializable
data class DiskTemplate(
    val tags: List<String>
)


@kotlinx.serialization.Serializable
data class VolumeTemplate(
    val volumeName: String,
    val volumeSize: Long,
    val blockSize: Long,
    val type: String,
    val enableDedup: Boolean,
    val compressionAlgorithm: String,
)

@kotlinx.serialization.Serializable
data class VolumeAccessGroupTemplate(
    val name: String,
    val volumes: List<String>,
    val initiators: List<String>
)


object UserDataManagement {
    val lock = Mutex()
    val folder = DATA_FILES.findSub("userdata")

    fun readAll(uid: String): MutableList<UserData> {
        return folder.findFile("$uid.yaml").deserializeList<UserData>()
    }

    inline fun <reified T : UserData> getAll(uid: String): MutableList<T> {
        return readAll(uid).filterIsInstanceTo(mutableListOf())
    }

    inline fun <reified T : UserData> first(uid: String): T? {
        return readAll(uid).firstOrNull{ it is T } as T?
    }

    suspend fun save(uid: String, userData: UserData){
        lock.withLock {
            folder.findFile("$uid.yaml").writeList(readAll(uid).apply {
                removeIf { it.id == userData.id }
                add(0, userData)
            })
        }
    }

    suspend fun delete(uid: String, dataId: String){
        lock.withLock {
            folder.findFile("$uid.yaml").writeList(this.readAll(uid).apply{removeIf { it.id == dataId }})
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
                folder.findFile("$uid.yaml").writeList(all)
            }
        }
    }
}

object PortalAccessManagement{
    private val map = ConcurrentMap<String, MutableList<String>>()

    fun add(user: User, portal: String){
        map
            .getOrPut(user.uuid){Collections.synchronizedList(mutableListOf<String>())}
            .add(portal)
    }

    fun canAccess(user: User, portal: String): Boolean{
        return map.getOrDefault(user.uuid, emptyList()).contains(portal)
    }
}


inline fun <reified T : UserData> User.getAllData(): MutableList<T> = UserDataManagement.getAll(uuid)
suspend fun User.saveData(data: UserData) = UserDataManagement.save(uuid,data)
suspend inline fun <reified T:UserData> User.dataScope(block: (MutableList<T>) -> Boolean) = UserDataManagement.scope(uuid,block)