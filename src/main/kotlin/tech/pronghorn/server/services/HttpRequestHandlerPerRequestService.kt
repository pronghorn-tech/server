package tech.pronghorn.server.services

import com.http.HttpRequest
import mu.KotlinLogging
import tech.pronghorn.coroutines.awaitable.ServiceManagedCoroutineContext
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.http.HttpResponse
import tech.pronghorn.server.core.HttpRequestHandler

class HttpRequestHandlerPerRequestService(override val worker: CoroutineWorker,
                                          val requestHandler: HttpRequestHandler) : InternalQueueService<HttpRequest>() {
    override val logger = KotlinLogging.logger {}
    private val context = ServiceManagedCoroutineContext(this)
    private val writer by lazy(LazyThreadSafetyMode.NONE) {
        worker.requestInternalWriter<HttpResponse, ResponseWriterPerRequestService>()
    }

    override suspend fun process(request: HttpRequest): Boolean {
        val response = requestHandler.handleRequest(request)
        writer.addAsync(response)
        return true
    }
}
