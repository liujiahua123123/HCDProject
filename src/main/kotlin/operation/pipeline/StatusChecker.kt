package operation.pipeline

import operation.request.Pipeline
import operation.request.Requester
import operation.request.Response
import utils.HttpStatusException


object StatusChecker: Pipeline {
    override suspend fun beforeRequest(request: Requester, data: Any) {

    }

    @Throws(HttpStatusException::class)
    override suspend fun afterResponse(request: Requester, response: Response) {
        if(response.statusCode >= 400){
            throw HttpStatusException("Http Status = " + response.statusCode + " body = " + response.body)
        }
    }


}