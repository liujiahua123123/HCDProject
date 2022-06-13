package operation.request

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import server.ServerJson
import sslTrustManager
import utils.RequestBodyException
import utils.ResponseBodyException
import utils.asMap

val client = HttpClient(CIO) {
    install(ContentNegotiation){
        json(Json{
            ignoreUnknownKeys=true
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        })
    }
    engine {
        https{
            trustManager = sslTrustManager
        }
    }
}



interface Pipeline{
    suspend fun beforeRequest(request: Requester, data: Any)

    suspend fun afterResponse(request: Requester, response: Response)
}

class Requester(){
    enum class Method(val ktorMethod: HttpMethod){
        GET(HttpMethod.Get),
        POST(HttpMethod.Post),
        PUT(HttpMethod.Put),
        DELETE(HttpMethod.Delete),

        FORM_POST(HttpMethod.Post)
    }

    var method: Method = Method.GET

    var protocol: URLProtocol = URLProtocol.HTTPS

    //domain should include port
    var domain: String = ""

    private val path: MutableList<String> = mutableListOf()
    private fun addPathSegment(parameter: String){
        path.add(parameter)
    }

    lateinit var data: Any

    private val headers: MutableList<Pair<String,String>> = mutableListOf()
    fun addHeader(key:String, value:String) = headers.add(Pair(key,value))

    fun addPathParameter(path: String){
        this.path.addAll(path.split("/").filter { it.isNotEmpty() })
    }

    private val pipelines: MutableList<Pipeline> = mutableListOf()
    fun addPipeline(pipeline: Pipeline) = pipelines.add(pipeline)

    suspend inline fun <reified T:Any> send(data: T): Response {
        if(this.method == Method.GET || this.method == Method.FORM_POST){
            return sendImpl(data.asMap())
        }else{
            return sendImpl(data)
        }
    }

    fun buildURL(builder:URLBuilder = URLBuilder()):URLBuilder{
        return builder.also{
            it.host = this@Requester.domain.substringBeforeLast(":")
            if(this@Requester.domain.contains(":")) {
                it.port = this@Requester.domain.substringAfterLast(":").toInt()
            }
            it.pathSegments = this@Requester.path
            it.protocol = this@Requester.protocol
        }
    }

    /**
     * this function should not get called directly
     */
    suspend fun sendImpl(data: Any): Response {

        this.data = data

        pipelines.forEach {
            it.beforeRequest(this@Requester, data)
        }


        val resp = client.request{

            this.method = this@Requester.method.ktorMethod

            buildURL(this.url)

            this@Requester.headers.forEach {
                this.headers.append(it.first,it.second)
            }

            /* Type Erase Warning */

            when (this@Requester.method) {
                Method.FORM_POST -> {
                    try {
                        val content: Parameters = Parameters.build {
                            (data as Map<*, *>).forEach {
                                append(it.key as String, it.value as String)
                            }
                        }
                        val form = FormDataContent(content)
                        setBody(form)
                    }catch (e: Exception){
                        throw RequestBodyException("Failed to build FORM POST request with given data $data").apply {
                            addSuppressed(e)
                        }
                    }
                }

                Method.GET -> {
                    try {
                        (data as Map<*, *>).forEach {
                            parameter(it.key as String, it.value)
                        }
                    }catch (e: Exception){
                        throw RequestBodyException("Failed to build GET request with given data $data").apply {
                            addSuppressed(e)
                        }
                    }
                }

                else -> {
                    contentType(ContentType.Application.Json)
                    //content negotiation
                    setBody(data)
                }
            }
        }

        return Response(
            resp.status.value,
            resp.bodyAsText()
        ).apply {
            pipelines.forEach {
                it.afterResponse(this@Requester,this)
            }
        }
    }

}


data class Response(
    val statusCode: Int,
    val body: String
){
    inline fun <reified T:Any> parse():T{
        try {
            return ServerJson.decodeFromString(this.body)
        }catch (e:Exception){
            throw ResponseBodyException("Failed to deserialize response as ${T::class.simpleName}, raw data = $body").apply {
                addSuppressed(e)
            }
        }
    }
}

