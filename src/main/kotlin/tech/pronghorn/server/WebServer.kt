package tech.pronghorn.server

import tech.pronghorn.coroutines.awaitable.QueueWriter
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.plugins.concurrentMap.ConcurrentMapPlugin
import tech.pronghorn.plugins.concurrentSet.ConcurrentSetPlugin
import tech.pronghorn.server.config.WebServerConfig
import tech.pronghorn.server.core.HttpRequestHandler
import tech.pronghorn.server.services.ServerConnectionCreationService
import java.io.IOException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.locks.ReentrantLock

class WebServer(val config: WebServerConfig,
                val handler: HttpRequestHandler) {
    private val logger = mu.KotlinLogging.logger {}
    private val serverSocket: ServerSocketChannel = ServerSocketChannel.open()
    private val workers = ConcurrentSetPlugin.get<WebServerWorker>()
    private val workerSocketWriters = ConcurrentMapPlugin.get<WebServerWorker, QueueWriter<SocketChannel>>()
    private var lastWorkerID = 0
    private val acceptLock = ReentrantLock()
    init { serverSocket.configureBlocking(false) }
    val serverBytes = config.serverName.toByteArray(Charsets.US_ASCII)
    var isRunning = false

    fun start() {
        logger.debug { "Starting server on ${config.address} with $${config.workerCount} workers" }
        isRunning = true
        serverSocket.socket().bind(config.address, 128)

        for (x in 1..config.workerCount) {
            val worker = WebServerWorker(this, config, handler)
            workerSocketWriters.put(worker, worker.requestSingleExternalWriter<SocketChannel, ServerConnectionCreationService>())
            workers.add(worker)
        }

        workers.forEach(CoroutineWorker::start)
    }

    fun shutdown() {
        logger.info("Server on ${config.address} shutting down")
        isRunning = false
        try {
            workers.forEach(WebServerWorker::shutdown)
        }
        catch (ex: Exception){
            ex.printStackTrace()
        }
        finally {
            logger.info("Closing server socket.")
            serverSocket.close()
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

    fun registerAcceptWorker(selector: Selector): SelectionKey {
        return serverSocket.register(selector, SelectionKey.OP_ACCEPT)
    }

    private fun getBestWorker(): WebServerWorker {
        return workers.elementAt(lastWorkerID++ % config.workerCount)
    }

//    internal fun attemptAccept() {
//        if(acceptLock.tryLock()) {
//            try {
//                val acceptedSocket: SocketChannel? = serverSocket.accept()
//                if (acceptedSocket != null) {
//                    acceptedSocket.configureBlocking(false)
//                    var handled = false
//                    while (!handled) {
//                        val worker = getBestWorker()
//                        val workerWriter = workerSocketWriters.getOrPut(worker, { worker.requestSingleExternalWriter<SocketChannel, ServerConnectionCreationService>() })
//                        handled = workerWriter.offer(acceptedSocket)
//                    }
//                }
//            }
//            finally {
//                acceptLock.unlock()
//            }
//        }
//    }

    val acceptGrouping = 128

    internal fun attemptAccept() {
        if(acceptLock.tryLock()) {
            try {
                logger.debug { "Accepting connections..." }
                var acceptedSocket: SocketChannel? = serverSocket.accept()
                var accepted = 0
                while (acceptedSocket != null) {
                    accepted += 1
                    acceptedSocket.configureBlocking(false)
                    var handled = false
                    while (!handled) {
                        val worker = getBestWorker()
                        val workerWriter = workerSocketWriters.getValue(worker)
                        handled = workerWriter.offer(acceptedSocket)
                    }
                    if(accepted > acceptGrouping){
                        break
                    }
                    acceptedSocket = serverSocket.accept()
                }
                logger.debug { "Accepted $accepted connections." }
            }
            catch (ex: IOException){
                // no-op
            }
            finally {
                acceptLock.unlock()
            }
        }
    }

//    override fun finalize() {
//        super.finalize()
//        if(serverSocket.isOpen){
//            println("FAILED TO CLOSE SOCKET WebServer")
//            System.exit(1)
//        }
//    }
}
