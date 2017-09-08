package tech.pronghorn.server

import mu.KotlinLogging
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.plugins.concurrentSet.ConcurrentSetPlugin
import tech.pronghorn.server.config.HttpServerConfig
import tech.pronghorn.server.handlers.HttpRequestHandler
import tech.pronghorn.server.services.MultiSocketManagerService
import tech.pronghorn.server.services.SingleSocketManagerService
import tech.pronghorn.server.services.SocketManagerService
import java.net.InetSocketAddress
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.locks.ReentrantLock

class HttpServer(val config: HttpServerConfig) {
    private val logger = KotlinLogging.logger {}
    private val workers = ConcurrentSetPlugin.get<HttpServerWorker>()
    private val serverSocket by lazy {
        val socket = ServerSocketChannel.open()
        socket.configureBlocking(false)
        socket
    }
    private val acceptLock by lazy { ReentrantLock() }
    private val distributionStrategy by lazy { RoundRobinConnectionDistributionStrategy(workers) }

    var isRunning = false
        private set

    init {
        for (x in 1..config.workerCount) {
            val worker = HttpServerWorker(this, config)
            workers.add(worker)
        }
    }

    constructor(address: InetSocketAddress): this(HttpServerConfig(address))

    fun start() {
        logger.info { "Starting server with configuration: $config" }
        isRunning = true
        workers.forEach(CoroutineWorker::start)
    }

    fun shutdown() {
        logger.info { "Server at ${config.address} shutting down" }
        isRunning = false
        try {
            workers.forEach(HttpServerWorker::shutdown)
        }
        catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun getSocketManagerService(worker: HttpServerWorker,
                                selector: Selector): SocketManagerService {
        if(config.reusePort){
            return MultiSocketManagerService(worker, selector)
        }
        else {
            return SingleSocketManagerService(worker, selector, serverSocket, acceptLock, distributionStrategy)
        }
    }

    fun getConnectionCount(): Int = workers.map(HttpServerWorker::getConnectionCount).sum()


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
}
