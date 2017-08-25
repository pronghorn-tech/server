//package tech.pronghorn.server.services
//
//import mu.KotlinLogging
//import tech.pronghorn.coroutines.core.CoroutineWorker
//import tech.pronghorn.coroutines.service.InternalQueueService
//import tech.pronghorn.websocket.protocol.FrameWriter
//import tech.pronghorn.websocket.protocol.WebsocketFrame
//import tech.pronghorn.server.HttpConnection
//import java.nio.ByteBuffer
//
//class FrameWriterService(override val worker: CoroutineWorker) : InternalQueueService<HttpConnection>() {
//    override val logger = KotlinLogging.logger {}
//
//    override suspend fun process(connection: HttpConnection): Boolean {
//
//        return true
//    }
//
//    fun WebsocketFrame.encode(buffer: ByteBuffer): Unit {
//        FrameWriter.encodeFrame(this, buffer, buffer.position())
//    }
//}
