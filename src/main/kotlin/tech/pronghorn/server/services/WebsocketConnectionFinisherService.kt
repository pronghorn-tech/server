package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.websocket.core.WebsocketHandshaker
import tech.pronghorn.server.HttpClientConnection
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.*

class WebsocketConnectionFinisherService(override val worker: CoroutineWorker,
                                         val selector: Selector,
                                         private val randomGenerator: Random) : InternalQueueService<HttpClientConnection>() {
    override val logger = KotlinLogging.logger {}
    private val handshaker = WebsocketHandshaker()

    suspend override fun process(connection: HttpClientConnection): Boolean {
        if(connection.socket.finishConnect()){
            connection.updateInterestOps(SelectionKey.OP_READ)
            connection.sendHandshake(handshaker, randomGenerator)
        }
        else {
            connection.close("Unable to finish connecting.")
        }
        return true
    }
}
