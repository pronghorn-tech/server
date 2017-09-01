package tech.pronghorn.server

import mu.KotlinLogging
import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.core.InterWorkerMessage
import tech.pronghorn.coroutines.service.Service
import tech.pronghorn.http.ResponseHeaderWithValue
import tech.pronghorn.http.protocol.HttpResponseHeader
import tech.pronghorn.plugins.concurrentSet.ConcurrentSetPlugin
import tech.pronghorn.server.bufferpools.ConnectionBufferPool
import tech.pronghorn.server.config.HttpServerConfig
import tech.pronghorn.server.core.HttpRequestHandler
import tech.pronghorn.server.services.*
import tech.pronghorn.util.finder.ByteBacked
import tech.pronghorn.util.finder.ByteBackedFinder
import tech.pronghorn.util.finder.FinderGenerator
import tech.pronghorn.util.runAllIgnoringExceptions
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

sealed class HttpWorker : CoroutineWorker() {
    protected val connections = ConcurrentSetPlugin.get<HttpServerConnection>()

    val connectionBufferPool = ConnectionBufferPool(true)

    fun getConnectionCount(): Int = connections.size

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

    protected val connectionReadService = ConnectionReadService(this)

    protected val connectionReadServiceQueueWriter by lazy(LazyThreadSafetyMode.NONE) {
        connectionReadService.getQueueWriter()
    }

    protected val commonServices = listOf(
            connectionReadService
    )
}

/*
class HttpClientWorker(config: WebsocketClientConfig) : HttpWorker() {
    override val logger = KotlinLogging.logger {}
    private val connectionCreationService = ClientConnectionCreationService(this, selector, config.randomGeneratorBuilder())
    private val connectionFinisherService = WebsocketConnectionFinisherService(this, selector, config.randomGeneratorBuilder())
    private val connectionFinisherWriter by lazy(LazyThreadSafetyMode.NONE) {
        connectionFinisherService.getQueueWriter()
    }

    override val services: List<Service> = listOf(
            connectionCreationService,
            connectionFinisherService
    ).plus(commonServices)


    override fun processKey(key: SelectionKey): Unit {
        val attachment = key.attachment()
        when {
            key.isReadable && attachment is HttpClientConnection -> {
                if (!connectionReadServiceQueueWriter.offer(attachment)) {
                    // TODO: handle this properly
                    throw Exception("ConnectionReadService full!")
                }
                attachment.removeInterestOps(SelectionKey.OP_READ)
            }
            key.isConnectable && attachment is HttpClientConnection -> {
                if (!connectionFinisherWriter.offer(attachment)) {
                    // TODO: handle this properly
                    throw Exception("HandshakeService full!")
                }
                key.interestOps(0)
            }
            else -> throw Exception("Unexpected selection op.")
        }
    }
}
*/

data class URLHandlerMapping(val url: ByteArray,
                             val handler: HttpRequestHandler): ByteBacked {
    override val bytes = url
}

class HttpServerWorker(val server: HttpServer,
                       private val config: HttpServerConfig) : HttpWorker() {
    private val serverKey = server.registerAcceptWorker(selector)
    private val connectionCreationService = ServerConnectionCreationService(this, selector)
    private val httpRequestHandlerService = HttpRequestHandlerService(this)
    private val responseService = ResponseWriterPerRequestService(this)
    private val handlerService = HttpRequestHandlerPerRequestService(this)
    private val responseWriterService = ResponseWriterService(this)
    private val handlers = HashMap<Int, URLHandlerMapping>()
    private val gmt = ZoneId.of("GMT")
    private val commonHeaderCache = calculateCommonHeaderCache()
    private var latestDate = System.currentTimeMillis() / 1000
    val commonHeaderSize = commonHeaderCache.size

    private fun calculateCommonHeaderCache(): ByteArray {
        val dateBytes = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(gmt)).toByteArray(Charsets.US_ASCII)
        val dateHeader = ResponseHeaderWithValue(HttpResponseHeader.Date, dateBytes)
        val serverHeader = ResponseHeaderWithValue(HttpResponseHeader.Server, config.serverName)

        val bufferSize = (if(config.sendDateHeader) dateHeader.length else 0) + (if(config.sendServerHeader) serverHeader.length else 0)
        val buffer = ByteBuffer.allocate(bufferSize)

        if(config.sendServerHeader){
            serverHeader.writeHeader(buffer)
        }
        if(config.sendDateHeader){
            dateHeader.writeHeader(buffer)
        }

        return Arrays.copyOf(buffer.array(), buffer.capacity())
    }

    fun getCommonHeaders(): ByteArray {
        if(config.sendDateHeader){
            val now = System.currentTimeMillis() / 1000
            if (latestDate != now) {
                latestDate = now
                val dateBytes = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(gmt)).toByteArray(Charsets.US_ASCII)
                val dateStart = commonHeaderCache.size - dateBytes.size - 2
                var x = 0
                while (x < dateBytes.size) {
                    commonHeaderCache[dateStart + x] = dateBytes[x]
                    x += 1
                }
            }
        }

        return commonHeaderCache
    }

    override val services: List<Service> = listOf(
            connectionCreationService,
            httpRequestHandlerService,
            responseWriterService,
            handlerService,
            responseService
    ).plus(commonServices)

    private var handlerFinder: ByteBackedFinder<URLHandlerMapping> = FinderGenerator.generateFinder(handlers.values.toTypedArray())

    fun getHandler(urlBytes: ByteArray): HttpRequestHandler? = handlerFinder.find(urlBytes)?.handler

    fun addURLHandler(url: String,
                      handlerGenerator: () -> HttpRequestHandler){
        val urlBytes = url.toByteArray(Charsets.US_ASCII)
        val handler = handlerGenerator()

        handlers.put(Arrays.hashCode(urlBytes), URLHandlerMapping(urlBytes, handler))
        handlerFinder = FinderGenerator.generateFinder(handlers.values.toTypedArray())
    }

    override fun handleMessage(message: InterWorkerMessage): Boolean {
        if(message is RegisterURLHandlerMessage){
            addURLHandler(message.url, message.handlerGenerator)
            return true
        }

        return false
    }

    override fun processKey(key: SelectionKey) {
        if (key == serverKey && key.isAcceptable) {
            server.attemptAccept()
        } else if (key.isReadable) {
            val attachment = key.attachment()
            if (attachment is HttpServerConnection) {
                if (!attachment.isReadQueued) {
                    if (!connectionReadServiceQueueWriter.offer(attachment)) {
                        // TODO: handle this properly
                        throw Exception("ConnectionReadService full!")
                    }
                    attachment.isReadQueued = true
                }
            }
        } else {
            throw Exception("Unexpected readyOps for attachment : ${key.readyOps()} ${key.attachment()}")
        }
    }
}

class DummyWorker : HttpWorker() {
    override val services = emptyList<Service>()
    override fun processKey(key: SelectionKey) = TODO()
}
