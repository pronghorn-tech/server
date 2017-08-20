package tech.pronghorn.server

import mu.KotlinLogging
import tech.pronghorn.coroutines.awaitable.CoroutineFuture
import tech.pronghorn.coroutines.awaitable.InternalFuture
import tech.pronghorn.coroutines.awaitable.QueueWriter
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.plugins.concurrentMap.ConcurrentMapPlugin
import tech.pronghorn.plugins.concurrentSet.ConcurrentSetPlugin
import tech.pronghorn.server.config.WebsocketClientConfig
import tech.pronghorn.server.services.ClientConnectionCreationService
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

class PendingClientConnection(val socket: SocketChannel,
                              val promise: InternalFuture.InternalPromise<HttpClientConnection>)

class WebsocketClient(val config: WebsocketClientConfig) {
    private val logger = KotlinLogging.logger {}
    private val workers = ConcurrentSetPlugin.get<HttpClientWorker>()
    private val workerSocketWriters = ConcurrentMapPlugin.get<HttpClientWorker, QueueWriter<PendingClientConnection>>()
    private var lastWorkerID = 0
    private var hasStarted = false

    private fun start() {
        for (x in 1..config.workerCount) {
            val worker = HttpClientWorker(config)
            workers.add(worker)
        }

        workers.forEach(CoroutineWorker::start)
    }

    fun shutdown() {
        workers.forEach(WebWorker::shutdown)
    }

    fun getPendingConnectionCount(): Int = workers.map(WebWorker::getPendingConnectionCount).sum()

    fun getActiveConnectionCount(): Int = workers.map(WebWorker::getActiveConnectionCount).sum()

    private fun getBestWorker(): HttpClientWorker {
        return workers.elementAt(lastWorkerID++ % config.workerCount)
    }

    suspend fun connectAsync(address: InetSocketAddress): HttpClientConnection {
        if (!hasStarted) {
            start()
        }

        val socketChannel = SocketChannel.open()
        socketChannel.configureBlocking(false)
        socketChannel.connect(address)

        val worker = getBestWorker()
        val workerWriter = workerSocketWriters.getOrPut(worker, { worker.requestMultiExternalWriter<PendingClientConnection, ClientConnectionCreationService>() })
        val future = InternalFuture<HttpClientConnection>()

        val pending = PendingClientConnection(socketChannel, future.promise())
        workerWriter.offer(pending) // TODO: handle if this returns false
        return future.awaitAsync()
    }

    fun connect(address: InetSocketAddress): CoroutineFuture<HttpClientConnection> {
        if (!hasStarted) {
            start()
        }

        val socketChannel = SocketChannel.open()
        socketChannel.configureBlocking(false)
        socketChannel.connect(address)

        val worker = getBestWorker()
        val workerWriter = workerSocketWriters.getOrPut(worker, { worker.requestMultiExternalWriter<PendingClientConnection, ClientConnectionCreationService>() })

        val returnFuture = CoroutineFuture<HttpClientConnection>()
        val returnPromise = returnFuture.promise()
        val future = InternalFuture<HttpClientConnection>({ connection ->
            returnPromise.complete(connection)
        })
        val pending = PendingClientConnection(socketChannel, future.promise())
        workerWriter.offer(pending) // TODO: handle if this returns false
        return returnFuture
    }
}
