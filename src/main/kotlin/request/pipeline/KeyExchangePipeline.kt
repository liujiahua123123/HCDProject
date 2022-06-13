package request.pipeline

import operation.request.Pipeline
import operation.request.Requester
import operation.request.Response
import utils.KeyExchangeService

class KeyExchangePipeline() : Pipeline {
    override val priority: Int = 1
    override suspend fun beforeRequest(request: Requester, data: Any) {
        val key = KeyExchangeService.get(request.domain)
        request.addHeader("Authorization","bearer ${key.content}")
    }

    override suspend fun afterResponse(request: Requester, response: Response) {

    }

}