package tech.pronghorn.websocket

import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.plugins.concurrentSet.ConcurrentSetPlugin
import tech.pronghorn.server.HttpServerConnection
import tech.pronghorn.server.bufferpools.ConnectionBufferPool
import tech.pronghorn.util.runAllIgnoringExceptions

/*
sealed class WebsocketWorker : CoroutineWorker() {
    protected val pendingConnections = ConcurrentSetPlugin.get<WebsocketConnection>()
    protected val connections = ConcurrentSetPlugin.get<HttpServerConnection>()

    val handshakeBufferPool = ConnectionBufferPool(true)

    fun getConnectionCount(): Int = connections.size

    fun getPendingConnectionCount(): Int {
        return pendingConnections.size
    }

    fun getActiveConnectionCount(): Int {
        throw Exception("No longer valid")
//        allConnections.size - getPendingConnectionCount()
    }

    fun addConnection(connection: HttpServerConnection) {
        assert(isSchedulerThread())
        connections.add(connection)
    }

    fun removeConnection(connection: HttpServerConnection) {
        assert(isSchedulerThread())
        connections.remove(connection)
    }

    override fun onShutdown() {
        logger.info("Worker shutting down ${connections.size} connections")
        runAllIgnoringExceptions({ connections.forEach({ it.close("Server is shutting down.") }) })
    }

    protected val handshakeTimeoutService = HandshakeTimeoutService(this, pendingConnections, handshakeTimeout)
    protected val frameHandlerService = FrameHandlerService(this, frameHandler)
    protected val handshakeService = HandshakeService(this)

    protected val handshakeServiceQueueWriter by lazy(LazyThreadSafetyMode.NONE) {
        handshakeService.getQueueWriter()
    }

    protected val commonServices = listOf(
        handshakeService,
        handshakeTimeoutService,
        frameHandlerService,
    )
}
*/
