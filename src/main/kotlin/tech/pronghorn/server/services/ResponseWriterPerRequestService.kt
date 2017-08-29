package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.http.HttpExchange
import tech.pronghorn.http.HttpResponse

class ResponseWriterPerRequestService(override val worker: CoroutineWorker) : InternalQueueService<HttpResponse>() {
    override val logger = KotlinLogging.logger {}

    override suspend fun process(exchange: HttpResponse): Boolean {
//        TODO()
//        response.connection.writeResponse(response)
        return true
    }
}
