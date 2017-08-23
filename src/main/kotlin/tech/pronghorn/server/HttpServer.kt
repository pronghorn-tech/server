package tech.pronghorn.server

import tech.pronghorn.coroutines.awaitable.QueueWriter
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.core.InterWorkerMessage
import tech.pronghorn.http.protocol.HttpResponseHeader
import tech.pronghorn.plugins.concurrentMap.ConcurrentMapPlugin
import tech.pronghorn.plugins.concurrentSet.ConcurrentSetPlugin
import tech.pronghorn.server.config.HttpServerConfig
import tech.pronghorn.server.core.HttpRequestHandler
import tech.pronghorn.server.services.ServerConnectionCreationService
import java.io.IOException
import java.nio.channels.*
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
    init { serverSocket.configureBlocking(false) }
    val serverBytes = config.serverName.toByteArray(Charsets.US_ASCII)
    var isRunning = false
    private val serverDefaultHeaders = mutableMapOf(
            HttpResponseHeader.Server to { serverBytes },
            HttpResponseHeader.Date to HttpWorker::getDateHeaderValue
    )

    init {
        for (x in 1..config.workerCount) {
            val worker = HttpServerWorker(this, config)
            workerSocketWriters.put(worker, worker.requestSingleExternalWriter<SocketChannel, ServerConnectionCreationService>())
            workers.add(worker)
        }
    }

    fun setDefaultHeaders(headers: Map<HttpResponseHeader, () -> ByteArray>){
        serverDefaultHeaders.clear()
        serverDefaultHeaders.putAll(headers)
    }

    fun clearDefaultHeaders() {
        serverDefaultHeaders.clear()
    }

    fun addDefaultHeader(header: HttpResponseHeader,
                         value: () -> ByteArray){
        if(serverDefaultHeaders.contains(header)){
            throw Exception("Conflicting default header: $header")
        }
        serverDefaultHeaders.put(header, value)
    }

    fun getDefaultHeaders() = serverDefaultHeaders

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
        workers.map(HttpServerWorker::getPendingConnectionCount).sum()
    }

    fun getActiveConnectionCount(): Int {
        throw Exception("No longer valid")
        workers.map(HttpServerWorker::getActiveConnectionCount).sum()
    }

    fun getConnectionCount(): Int = workers.map(HttpServerWorker::getConnectionCount).sum()

    fun registerAcceptWorker(selector: Selector): SelectionKey {
        return serverSocket.register(selector, SelectionKey.OP_ACCEPT)
    }

    private fun getBestWorker(): HttpServerWorker {
        return workers.elementAt(lastWorkerID++ % config.workerCount)
    }

    fun registerUrl(url: String,
                    handlerGenerator: () -> HttpRequestHandler){
        workers.forEach { worker ->
            worker.sendInterWorkerMessage(RegisterURLHandlerMessage(url, handlerGenerator))
        }
    }

    fun registerUrl(url: String,
                    handler: HttpRequestHandler) {
        registerUrl(url, { handler })
    }

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
                    acceptedSocket.socket().tcpNoDelay = true
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
//            println("FAILED TO CLOSE SOCKET HttpServer")
//            System.exit(1)
//        }
//    }
}
