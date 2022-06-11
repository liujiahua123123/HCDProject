package operation.request

import JVMIndex
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import sslTrustManager
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
    fun beforeRequest(request: Request)

    fun <T:Any> afterResponse(request: Request, response: Response)
}

class Request(){
    enum class Method(val ktorMethod: HttpMethod){
        GET(HttpMethod.Get),
        POST(HttpMethod.Post),
        PUT(HttpMethod.Put),
        DELETE(HttpMethod.Delete),

        FORM_POST(HttpMethod.Post)
    }

    var method: Request.Method = Method.GET

    var protocol: URLProtocol = URLProtocol.HTTPS

    //domain should include port
    var domain: String = ""

    private val path: MutableList<String> = mutableListOf()
    private fun addPathSegment(parameter: String){
        path.add(parameter)
    }

    private val headers: MutableList<Pair<String,String>> = mutableListOf()
    fun addHeader(key:String, value:String) = headers.add(Pair(key,value))

    fun addPathParameter(path: String){
        this.path.addAll(path.split("/").filter { it.isNotEmpty() })
    }

    private val pipelines: MutableList<Pipeline> = mutableListOf()
    fun addPipeline(pipeline: Pipeline) = pipelines.add(pipeline)

    suspend inline fun <reified T:Any> send(data: T):Response{
        if(this.method == Method.GET || this.method == Method.FORM_POST){
            return sendImpl(data.asMap())
        }else{
            return sendImpl(data)
        }
    }

    /**
     * this function should not get called directly
     */
    suspend fun sendImpl(data: Any): Response{
        pipelines.forEach {
            it.beforeRequest(this)
        }

        val resp = client.request{

            this.method = this@Request.method.ktorMethod

            url {
                it.host = this@Request.domain.substringBeforeLast(":")
                if(this@Request.domain.contains(":")) {
                    it.port = this@Request.domain.substringAfterLast(":").toInt()
                }
                it.pathSegments = this@Request.path
                it.protocol = this@Request.protocol
            }

            this@Request.headers.forEach {
                this.headers.append(it.first,it.second)
            }

            when (this@Request.method) {
                Method.FORM_POST -> {
                    val content:Parameters = Parameters.build {
                        (data as Map<*, *>).forEach {
                            append(it.key as String, it.value as String)
                        }
                    }
                    val form = FormDataContent(content)
                    println(form.formData)
                    setBody(form)
                }

                Method.GET -> {
                    contentType(ContentType.Application.Json)
                    (data as Map<*, *>).forEach {
                        parameter(it.key as String,it.value)
                    }
                }

                else -> {
                    //auto content negotiation
                    contentType(ContentType.Application.Json)
                    setBody(data)
                }
            }
        }

        return Response(
            resp.status.value,
            resp.bodyAsText()
        )
    }

}


data class Response(
    val statusCode: Int,
    val body: String
)


@Serializable
data class LoginReq(
    val grant_type: String = "password",
    val password: String  = "Hello123!",
    val username: String = "admin",
    val int: Int = 0
)
suspend fun main(){
    val r = Request()
    r.domain = "172.16.4.248:8443"
    r.method = Request.Method.FORM_POST
    r.addPathParameter("/oauth/token")

    r.addHeader("Authorization","Basic aGNkLWNsaWVudDpoY2Qtc2VjcmV0")
    val resp = r.send(LoginReq())
    println(resp.statusCode)
    println(resp.body)
}