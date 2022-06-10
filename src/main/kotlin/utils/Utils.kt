package utils

import kotlin.reflect.full.memberProperties


inline fun <reified T : Any> T.asMap() : Map<String, Any?> {
    val props = T::class.memberProperties.associateBy { it.name }
    return props.keys.associateWith { props[it]?.get(this) }
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
