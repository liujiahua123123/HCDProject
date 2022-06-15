package operation.request

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import server.ServerJson
import sslTrustManager
import utils.RequestBodyException
import utils.ResponseBodyException
import utils.asMap
import utils.forEachNonNullPair
import java.util.PriorityQueue

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        })
    }
    engine {
        https {
            trustManager = sslTrustManager
        }
    }
}


interface Pipeline {
    val priority: Int
    suspend fun beforeRequest(request: Requester, data: Any)

    suspend fun afterResponse(request: Requester, response: Response)

    suspend fun onThrowable(request: Requester, throwable: Throwable) {}
}

class Requester() {
    enum class Method(val ktorMethod: HttpMethod) {
        GET(HttpMethod.Get),
        POST(HttpMethod.Post),
        PUT(HttpMethod.Put),
        DELETE(HttpMethod.Delete),

        FORM_POST(HttpMethod.Post)
    }

    var method: Method = Method.GET

    var protocol: URLProtocol = URLProtocol.HTTPS

    //domain should include port
    var portal: String = ""

    private val path: MutableList<String> = mutableListOf()
    private fun addPathSegment(parameter: String) {
        path.add(parameter)
    }

    lateinit var data: Any

    private val headers: MutableList<Pair<String, String>> = mutableListOf()
    fun addHeader(key: String, value: String) = headers.add(Pair(key, value))

    fun addPathParameter(path: String) {
        this.path.addAll(path.split("/").filter { it.isNotEmpty() })
    }

    private val pipelines: PriorityQueue<Pipeline> = PriorityQueue { o1, o2 -> o1.priority - o2.priority }
    fun addPipeline(pipeline: Pipeline) = pipelines.add(pipeline)

    suspend inline fun <reified T : Any> send(data: T): Response {
        if (this.method == Method.GET || this.method == Method.FORM_POST) {
            return sendImpl(data.asMap())
        } else {
            return sendImpl(data)
        }
    }

    fun buildURL(builder: URLBuilder = URLBuilder()): URLBuilder {
        return builder.also {
            it.host = this@Requester.portal.substringBeforeLast(":")
            if (this@Requester.portal.removePrefix("https://").removePrefix("http://").contains(":")) {
                it.port = this@Requester.portal.substringAfterLast(":").toInt()
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

        try {
            val resp = client.request {

                this.method = this@Requester.method.ktorMethod

                buildURL(this.url)

                this@Requester.headers.forEach {
                    this.headers.append(it.first, it.second)
                }

                /* Type Erase Warning */

                try {
                    when (this@Requester.method) {
                        Method.FORM_POST -> {
                            setBody(FormDataContent(Parameters.build {
                                (data as Map<*, *>).forEachNonNullPair { key, v ->
                                    append(key, v.toString())
                                }
                            }))
                        }

                        Method.GET, Method.DELETE -> {
                            (data as Map<*, *>).forEachNonNullPair { key, v ->
                                parameter(key, v)
                            }
                        }

                        else -> {
                            contentType(ContentType.Application.Json)
                            //auto content negotiation
                            setBody(data)
                        }
                    }
                } catch (e: Throwable) {
                    throw RequestBodyException("Failed to build ${this.method} request with $data").apply {
                        addSuppressed(e)
                    }
                }
            }

            return Response(
                resp.status.value,
                resp.bodyAsText()
            ).apply {
                pipelines.forEach {
                    it.afterResponse(this@Requester, this)
                }
            }
        } catch (e: Throwable) {
            pipelines.forEach {
                it.onThrowable(this@Requester, e)
            }
            throw e
        }
    }
}


data class Response(
    val statusCode: Int,
    val body: String
) {
    inline fun <reified T : Any> parse(): T {
        try {
            return ServerJson.decodeFromString(this.body)
        } catch (e: Exception) {
            throw ResponseBodyException("Failed to deserialize response as ${T::class.simpleName}, raw data = $body").apply {
                addSuppressed(e)
            }
        }
    }
}

