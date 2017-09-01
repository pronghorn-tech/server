package tech.pronghorn.server.services

import mu.KLogger
import mu.KotlinLogging
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.server.HttpServerConnection

class ResponseWriterService(override val worker: CoroutineWorker) : InternalQueueService<HttpServerConnection>() {
    override suspend fun process(connection: HttpServerConnection): Boolean {
        return connection.writeResponses()
    }
}
