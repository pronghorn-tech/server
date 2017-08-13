package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.http.HttpResponse
import tech.pronghorn.websocket.protocol.FrameWriter
import tech.pronghorn.websocket.protocol.WebsocketFrame
import java.nio.ByteBuffer

class ResponseWriterPerRequestService(override val worker: CoroutineWorker) : InternalQueueService<HttpResponse>() {
    override val logger = KotlinLogging.logger {}

    override suspend fun process(response: HttpResponse): Boolean {
        response.connection.writeResponse(response)
        return true
    }

    fun WebsocketFrame.encode(buffer: ByteBuffer): Unit {
        FrameWriter.encodeFrame(this, buffer, buffer.position())
    }
}
