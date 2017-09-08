package tech.pronghorn.server.services

import tech.pronghorn.coroutines.service.MultiWriterExternalQueueService
import tech.pronghorn.server.HttpServerConnection
import tech.pronghorn.server.HttpServerWorker
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

class ServerConnectionCreationService(override val worker: HttpServerWorker,
                                      private val selector: Selector) : MultiWriterExternalQueueService<SocketChannel>() {
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    suspend override fun process(socket: SocketChannel) {
        socket.configureBlocking(false)
        val selectionKey = socket.register(selector, SelectionKey.OP_READ)
        val connection = HttpServerConnection(worker, socket, selectionKey)
        worker.addConnection(connection)
        selectionKey.attach(connection)
    }
}

