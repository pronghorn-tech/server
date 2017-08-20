package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.awaitable.ServiceManagedCoroutineContext
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.http.HttpRequest
import tech.pronghorn.http.HttpResponse
import tech.pronghorn.server.HttpServerWorker

class HttpRequestHandlerPerRequestService(override val worker: HttpServerWorker) : InternalQueueService<HttpRequest>() {
    override val logger = KotlinLogging.logger {}
    private val context = ServiceManagedCoroutineContext(this)
    private val writer by lazy(LazyThreadSafetyMode.NONE) {
        worker.requestInternalWriter<HttpResponse, ResponseWriterPerRequestService>()
    }

    override suspend fun process(request: HttpRequest): Boolean {
        val handler = worker.getHandler(request.url.getPathBytes())
        if(handler == null){
            TODO()
        }
        else {
            val response = handler.handleRequest(request)
            writer.addAsync(response)
        }
        return true
    }
}
