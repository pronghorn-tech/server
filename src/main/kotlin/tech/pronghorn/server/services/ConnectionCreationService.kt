package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.service.MultiWriterExternalQueueService
import tech.pronghorn.coroutines.service.SingleWriterExternalQueueService
import tech.pronghorn.server.*
import tech.pronghorn.websocket.core.WebsocketHandshaker
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.*

class ServerConnectionCreationService(override val worker: HttpServerWorker,
                                      private val selector: Selector) : SingleWriterExternalQueueService<SocketChannel>() {
    override val logger = KotlinLogging.logger {}
    private val handshaker = WebsocketHandshaker()

    suspend override fun process(socket: SocketChannel) {
        socket.configureBlocking(false)
        val selectionKey = socket.register(selector, SelectionKey.OP_READ)
        val connection = HttpServerConnection(worker, socket, selectionKey)
        worker.addConnection(connection)
        selectionKey.attach(connection)
//        connection.attemptHandshake(handshaker)
    }
}

class ClientConnectionCreationService(override val worker: HttpClientWorker,
                                      private val selector: Selector,
                                      private val randomGenerator: Random) : MultiWriterExternalQueueService<PendingClientConnection>() {
    init { TODO("Should this be a SingleWriterExternalQueueService?") }
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

