package utils

import kotlin.reflect.full.memberProperties


inline fun <reified T : Any> T.asMap() : Map<String, Any?> {
    val props = T::class.memberProperties.associateBy { it.name }
    return props.keys.associateWith { (props[it]?.get(this)) }
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