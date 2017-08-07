package tech.pronghorn.server

import org.jctools.maps.NonBlockingHashMap
import org.jctools.maps.NonBlockingHashSet
import tech.pronghorn.coroutines.awaitable.QueueWriter
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.service.Service
import tech.pronghorn.server.core.HttpRequestHandler
import tech.pronghorn.server.services.ServerConnectionCreationService
import tech.pronghorn.server.config.WebServerConfig
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

class WebServer(val config: WebServerConfig,
                val handler: HttpRequestHandler) : CoroutineWorker() {
    override val logger = mu.KotlinLogging.logger {}
    private val serverSocket: ServerSocketChannel = ServerSocketChannel.open()
    private val workers = NonBlockingHashSet<WebServerWorker>()
    private val workerSocketWriters = NonBlockingHashMap<WebServerWorker, QueueWriter<SocketChannel>>()
    private var lastWorkerID = 0
    init { serverSocket.configureBlocking(false) }
    private val serverKey = serverSocket.register(selector, 0)
    val serverBytes = config.serverName.toByteArray(Charsets.US_ASCII)

    override val services: List<Service> = emptyList()

    override fun processKey(key: SelectionKey) {
        accept()
    }

    override fun onStart() {
        logger.debug("Starting server on ${config.address} with $${config.workerCount} workers")
        serverSocket.socket().bind(config.address, 64)
        serverKey.interestOps(SelectionKey.OP_ACCEPT)

        for (x in 1..config.workerCount) {
            val worker = WebServerWorker(config, handler)
            workers.add(worker)
        }

        workers.forEach(CoroutineWorker::start)
    }

    override fun onShutdown() {
        logger.info("Server on ${config.address} shutting down")
        try {
            serverSocket.close()
        }
        finally {
            workers.forEach(WebServerWorker::shutdown)
        }
    }

    fun getPendingConnectionCount(): Int {
        throw Exception("No longer valid")
        workers.map(WebServerWorker::getPendingConnectionCount).sum()
    }

    fun getActiveConnectionCount(): Int {
        throw Exception("No longer valid")
        workers.map(WebServerWorker::getActiveConnectionCount).sum()
    }

    fun getConnectionCount(): Int = workers.map(WebServerWorker::getConnectionCount).sum()

    private fun getBestWorker(): WebServerWorker {
        return workers.elementAt(lastWorkerID++ % config.workerCount)
    }

    private fun accept() {
        logger.debug("Accepting connections...")
        var acceptedSocket: SocketChannel? = serverSocket.accept()
        var accepted = 0
        while (acceptedSocket != null) {
            accepted += 1
            acceptedSocket.configureBlocking(false)
            var handled = false
            while(!handled){
                val worker = getBestWorker()
                val workerWriter = workerSocketWriters.getOrPut(worker, { worker.requestMultiExternalWriter<SocketChannel, ServerConnectionCreationService>() })
                handled = workerWriter.offer(acceptedSocket)
            }
            acceptedSocket = serverSocket.accept()
        }
        logger.debug("Accepted $accepted connections.")
    }

//    override fun finalize() {
//        super.finalize()
//        if(serverSocket.isOpen){
//            println("FAILED TO CLOSE SOCKET WebServer")
//            System.exit(1)
//        }
//    }
}
