package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.websocket.core.WebsocketHandshaker
import tech.pronghorn.server.HttpConnection

class HandshakeService(override val worker: CoroutineWorker) : InternalQueueService<HttpConnection>() {
    override val logger = KotlinLogging.logger {}

    private val handshaker = WebsocketHandshaker()

    override suspend fun process(connection: HttpConnection): Boolean {
        connection.attemptHandshake(handshaker)
        return true
    }
}
