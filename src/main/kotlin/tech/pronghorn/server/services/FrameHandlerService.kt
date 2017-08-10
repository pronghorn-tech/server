package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.awaitable.ServiceManagedCoroutineContext
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.core.myRun
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.websocket.protocol.WebsocketFrame
import tech.pronghorn.websocket.core.FrameHandler

class FrameHandlerService(override val worker: CoroutineWorker,
                          val frameHandler: FrameHandler) : InternalQueueService<WebsocketFrame>() {
    override val logger = KotlinLogging.logger {}

    private val context = ServiceManagedCoroutineContext(this)

    override suspend fun process(frame: WebsocketFrame): Boolean {
        myRun(context){
            frameHandler.handleFrame(frame)
        }
        return true
    }
}
