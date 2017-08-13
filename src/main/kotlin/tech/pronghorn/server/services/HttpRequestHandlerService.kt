package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.server.HttpConnection
import tech.pronghorn.server.core.HttpRequestHandler

class HttpRequestHandlerService(override val worker: CoroutineWorker,
                                val requestHandler: HttpRequestHandler) : InternalQueueService<HttpConnection>() {
    override val logger = KotlinLogging.logger {}
//    private val context = ServiceManagedCoroutineContext(this)

    override suspend fun process(connection: HttpConnection): Boolean {
//        myRun(context){
        connection.handleRequests(requestHandler)

        return true
    }
}
