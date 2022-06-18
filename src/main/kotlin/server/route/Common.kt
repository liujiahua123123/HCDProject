package server.route

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import server.*
import utils.Job
import utils.OperationExecutor
import utils.STATIC_FILES

fun Routing.commonRoute(){

    get("/utils.js"){
        call.respondFile(STATIC_FILES.findFile("utils.js"))
    }
    get("/logo.svg"){
        call.respondFile(STATIC_FILES.findFile("logo.svg"))
    }

    handleDataPost("/trace"){
        ifLogin {
            val data = call.readDataRequest<TraceRequest>()
            val job: Job<*> = OperationExecutor.get(data.traceId)?: userInputError("Task Id Not Found")
            call.respondTraceable(job)
        }
    }


}

@kotlinx.serialization.Serializable
data class TraceRequest(
    val traceId: String
)