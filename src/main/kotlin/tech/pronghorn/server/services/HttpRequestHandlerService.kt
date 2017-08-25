package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.server.HttpServerConnection
import tech.pronghorn.server.HttpServerWorker

class HttpRequestHandlerService(override val worker: HttpServerWorker) : InternalQueueService<HttpServerConnection>() {
    override val logger = KotlinLogging.logger {}
//    private val context = ServiceManagedCoroutineContext(this)

    override suspend fun process(connection: HttpServerConnection): Boolean {
        connection.handleRequests(worker)

        return true
    }
}
