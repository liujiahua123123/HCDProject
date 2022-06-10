package operation.request

import JVMIndex
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import sslTrustManager

val client = HttpClient(CIO) {
    install(ContentNegotiation){
        json(Json{
            ignoreUnknownKeys=true
            prettyPrint = true
            isLenient = true
        })
    }
    engine {
        https{
            trustManager = sslTrustManager
        }
    }
}



interface Pipeline{
    fun beforeRequest(request: Request, block: HttpRequestBuilder.() -> Unit)
    fun <T:Any> afterResponse(request: Request, response: Response<T>)
}

class Request(){
    private val pipelines: MutableList<Pipeline> = mutableListOf()
    fun addPipeline(pipeline: Pipeline) = pipelines.add(pipeline)
}

data class Response<T:Any>(
    val statusCode: Int,
    val body: T
)

