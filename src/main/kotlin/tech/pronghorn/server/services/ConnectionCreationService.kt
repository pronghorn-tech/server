package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.service.MultiWriterExternalQueueService
import tech.pronghorn.websocket.core.WebsocketHandshaker
import tech.pronghorn.server.*
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.*

class ServerConnectionCreationService(override val worker: WebServerWorker,
                                      private val selector: Selector) : MultiWriterExternalQueueService<SocketChannel>() {
    override val logger = KotlinLogging.logger {}
    private val handshaker = WebsocketHandshaker()

    suspend override fun process(socket: SocketChannel) {
        val selectionKey = socket.register(selector, SelectionKey.OP_READ)
        val connection = HttpServerConnection(worker, socket, selectionKey)
        worker.addConnection(connection)
        selectionKey.attach(connection)
//        connection.attemptHandshake(handshaker)
    }
}

class ClientConnectionCreationService(override val worker: WebClientWorker,
                                      private val selector: Selector,
                                      private val randomGenerator: Random) : MultiWriterExternalQueueService<PendingClientConnection>() {
    override val logger = KotlinLogging.logger {}
    private val handshaker = WebsocketHandshaker()

    suspend override fun process(pending: PendingClientConnection) {
        Thread.sleep(1000)
        val selectionKey = pending.socket.register(selector, 0)
        val connection = HttpClientConnection(worker, pending.socket, selectionKey, pending.promise)
        worker.addConnection(connection)
        selectionKey.attach(connection)

        if (pending.socket.finishConnect()) {
            connection.sendHandshake(handshaker, randomGenerator)
            selectionKey.interestOps(SelectionKey.OP_READ)
        }
        else {
            selectionKey.interestOps(SelectionKey.OP_CONNECT)
        }
    }
}

