package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.awaitable.ServiceManagedCoroutineContext
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.http.HttpExchange
import tech.pronghorn.http.HttpResponse
import tech.pronghorn.server.HttpServerWorker

class HttpRequestHandlerPerRequestService(override val worker: HttpServerWorker) : InternalQueueService<HttpExchange>() {
    private val context = ServiceManagedCoroutineContext(this)
    private val writer by lazy(LazyThreadSafetyMode.NONE) {
        worker.requestInternalWriter<HttpResponse, ResponseWriterPerRequestService>()
    }

    override suspend fun process(exchange: HttpExchange): Boolean {
        val handler = worker.getHandler(exchange.requestUrl.getPathBytes())
        if(handler == null){
            TODO()
        }
        else {
            TODO()
//            val response = handler.handle(exchange)
//            writer.addAsync(response)
        }
        return true
    }
}
