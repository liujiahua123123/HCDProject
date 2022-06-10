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

