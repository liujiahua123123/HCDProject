package utils

import kotlinx.serialization.*
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import operation.disk.DiskRemoveTagReq
import java.io.File
import java.security.MessageDigest
import kotlin.reflect.full.memberProperties


inline fun <reified T : Any> T.asMap() : Map<String, Any?> {
    val props = T::class.memberProperties.filter { !it.annotations.contains(Transient()) }.associateBy { it.name }
    return props.keys.associateWith { (props[it]?.get(this)) }
}

fun Map<*, *>.forEachNonNullPair(block: (String, Any) -> Unit){
    this.forEach { (key, v) ->
        if(v != null){
            block(key as String, v)
        }
    }
}

inline fun <reified T : Any> T.eachProperty(block: (String, Any?) -> Unit) {
    val props = T::class.memberProperties.associateBy {
        it.name
    }
    props.forEach {
        block(it.key, props[it.key]?.get(this))
    }
}


private val REGEX_X = Regex("x")
private val RANDOM_CHAR_CANDIDATES = arrayOf("a", "b", "c", "d", "e", "f", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9")

fun createUuid4(): String = //UUID.randomUUID().toString()
    "xxxxxxxx-xxxx-4xxx-xxxx-xxxxxxxxxxxx".replace(REGEX_X) { RANDOM_CHAR_CANDIDATES.random() }


class RequestBodyException(override val message: String):Exception(message)
class ResponseBodyException(override val message: String):Exception(message)
class HttpStatusException(override val message: String):Exception(message)


enum class LogColorOutputType(){
    LinuxCode,
    HTML,
    NOTHING
}
enum class LogColor(private val linuxColorCode: String, private val htmlColorCode: String){
    RESET("\u001B[0m", ""),
    RED("\u001B[31m","red"),
    GREEN("\u001B[32m","green"),
    YELLOW("\u001B[33m","yellow"),
    TEAL("\u001B[36m","teal")
    ;

    fun toString(type: LogColorOutputType): String {
        return when(type){
            LogColorOutputType.LinuxCode -> this.linuxColorCode
            LogColorOutputType.HTML -> this.htmlColorCode
            LogColorOutputType.NOTHING -> ""
        }
    }

    override fun toString(): String {
        return this.toString(LogColorOutputType.LinuxCode)
    }
}



private val FILE_ENTRY = System.getProperty("user.dir") + "/"
object DefaultFolder:FolderEntryImpl(FILE_ENTRY)
val STATIC_FILES = DefaultFolder.findSub("static")
val DATA_FILES = DefaultFolder.findSub("data")


interface FolderEntry{
    fun findFile(name:String): File = File(findPath(name)).also {
        if(!it.exists()){
            it.createNewFile()
        }
    }

    fun findPath(name:String):String

    fun findSub(name:String):FolderEntry

    operator fun invoke(name:String): File = findFile(name)

    fun listSubs():List<String>

    fun hasSub(name:String):Boolean

    fun createSub(name:String):FolderEntry

    fun walkFiles():Sequence<File>

    override fun toString():String

    fun lastModified():Long{
        return this.walkFiles().maxByOrNull { it.lastModified() }?.lastModified()?:0L
    }
}

open class FolderEntryImpl(private val path:String):FolderEntry{

    override fun findPath(name: String): String {
        return path + name
    }

    override fun findSub(name: String): FolderEntry {
        val subName = path + name.removeSuffix("/") + "/"
        with(File(subName)) {
            if (!this.exists()) {
                this.mkdirs()
            }
        }
        return FolderEntryImpl(subName)
    }

    override fun listSubs(): List<String> {
        return File(path).listFiles()!!.filter { it.isDirectory }.map { it.name }.filter{it!= "." && it!= ".."}
    }

    override fun createSub(name:String):FolderEntry {
        File("$path$name/").mkdir()
        return findSub(name)
    }

    override fun walkFiles(): Sequence<File> {
        return File(this.path).walk().maxDepth(1).filter { it.isFile && it.name != "." && it.name!= ".."}
    }

    override fun hasSub(name: String): Boolean {
        return with(File(findPath(name))){
            this.exists() && this.isDirectory
        }
    }

    override fun toString(): String {
        return path
    }

}

fun String.md5(): String {
    try {
        val m = MessageDigest.getInstance("MD5")
        m.update(this.toByteArray(charset("UTF8")))
        val s = m.digest()
        var result = ""
        for (i in s.indices) {
            result += Integer.toHexString(0x000000FF and s[i].toInt() or -0x100).substring(6)
        }
        return result
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}


val FileJson = Json {
    this.ignoreUnknownKeys = true
    this.isLenient = true
    this.encodeDefaults = true
}

inline fun <reified T : Any> String.deserialize(): T = FileJson.decodeFromString(this)


inline fun <reified T : Any> T.serialize(format: StringFormat, serializer: KSerializer<T> = format.serializersModule.serializer()): String {
    return format.encodeToString(serializer, this)
}

inline fun <reified T:Any> File.writeList(list: List<T>){
    this.writeText(list.serialize(FileJson))
}

inline fun <reified T:Any> File.writeData(data: T){
    this.writeText(data.serialize(FileJson))
}

inline fun <reified T:Any> File.deserializeList():MutableList<T>{
    return deserialize{ mutableListOf() }
}

inline fun <reified T : Any> File.deserialize(defaultCreator:() -> T): T{
    val text = this.readText()
    if(text.isEmpty()){
        return defaultCreator()
    }
    return FileJson.decodeFromString(text)
}

fun String.akamaiHash():String = "" + this.map { it.toInt() }.filter { it in 49..127 }.sum()
