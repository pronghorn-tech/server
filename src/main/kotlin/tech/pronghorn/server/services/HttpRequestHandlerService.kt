package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.server.HttpConnection
import tech.pronghorn.server.HttpServerWorker

class HttpRequestHandlerService(override val worker: HttpServerWorker) : InternalQueueService<HttpConnection>() {
    override val logger = KotlinLogging.logger {}
//    private val context = ServiceManagedCoroutineContext(this)

    override suspend fun process(connection: HttpConnection): Boolean {
        connection.handleRequests(worker)

        return true
    }
}
