package tech.pronghorn.server

import tech.pronghorn.coroutines.awaitable.QueueWriter
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.core.InterWorkerMessage
import tech.pronghorn.http.protocol.HttpMethod
import tech.pronghorn.plugins.concurrentMap.ConcurrentMapPlugin
import tech.pronghorn.plugins.concurrentSet.ConcurrentSetPlugin
import tech.pronghorn.server.config.HttpServerConfig
import tech.pronghorn.server.core.HttpRequestHandler
import tech.pronghorn.server.services.ServerConnectionCreationService
import java.io.IOException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.locks.ReentrantLock

data class RegisterURLHandlerMessage(val url: String,
                                     val handlerGenerator: () -> HttpRequestHandler) : InterWorkerMessage

class HttpServer(val config: HttpServerConfig) {
    private val logger = mu.KotlinLogging.logger {}
    private val serverSocket: ServerSocketChannel = ServerSocketChannel.open()
    private val workers = ConcurrentSetPlugin.get<HttpServerWorker>()
    private val workerSocketWriters = ConcurrentMapPlugin.get<HttpServerWorker, QueueWriter<SocketChannel>>()
    private var lastWorkerID = 0
    private val acceptLock = ReentrantLock()
    var isRunning = false
        private set

    init {
        serverSocket.configureBlocking(false)
    }

    init {
        for (x in 1..config.workerCount) {
            val worker = HttpServerWorker(this, config)
            workerSocketWriters.put(worker, worker.requestSingleExternalWriter<SocketChannel, ServerConnectionCreationService>())
            workers.add(worker)
        }
    }

    fun start() {
        logger.debug { "Starting server on ${config.address} with ${config.workerCount} workers" }
        isRunning = true
        serverSocket.socket().bind(config.address, 128)

        workers.forEach(CoroutineWorker::start)
    }

    fun shutdown() {
        logger.info("Server on ${config.address} shutting down")
        isRunning = false
        try {
            workers.forEach(HttpServerWorker::shutdown)
        }
        catch (ex: Exception) {
            ex.printStackTrace()
        }
        finally {
            logger.info("Closing server socket.")
            serverSocket.close()
        }
    }

    fun getConnectionCount(): Int = workers.map(HttpServerWorker::getConnectionCount).sum()

    fun registerAcceptWorker(selector: Selector): SelectionKey {
        return serverSocket.register(selector, SelectionKey.OP_ACCEPT)
    }

    private fun getBestWorker(): HttpServerWorker {
        return workers.elementAt(lastWorkerID++ % config.workerCount)
    }

    fun registerUrlHandlerGenerator(url: String,
                                    handlerGenerator: () -> HttpRequestHandler) {
        workers.forEach { worker ->
            worker.sendInterWorkerMessage(RegisterURLHandlerMessage(url, handlerGenerator))
        }
    }

    fun registerUrlHandler(url: String,
                           handler: HttpRequestHandler) {
        registerUrlHandlerGenerator(url, { handler })
    }

    fun registerUrlHandler(url: String,
                           method: HttpMethod,
                           handler: HttpRequestHandler){
        TODO()
    }

    private val acceptGrouping = 128

    internal fun attemptAccept() {
        if (acceptLock.tryLock()) {
            try {
                logger.debug { "Accepting connections..." }
                var acceptedSocket: SocketChannel? = serverSocket.accept()
                var accepted = 0
                while (acceptedSocket != null) {
                    accepted += 1
                    acceptedSocket.configureBlocking(false)
                    acceptedSocket.socket().tcpNoDelay = true
                    var handled = false
                    while (!handled) {
                        val worker = getBestWorker()
                        val workerWriter = workerSocketWriters.getValue(worker)
                        handled = workerWriter.offer(acceptedSocket)
                    }
                    if (accepted > acceptGrouping) {
                        break
                    }
                    acceptedSocket = serverSocket.accept()
                }
                logger.debug { "Accepted $accepted connections." }
            }
            catch (ex: IOException) {
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
//            println("FAILED TO CLOSE SOCKET HttpServer")
//            System.exit(1)
//        }
//    }
}
