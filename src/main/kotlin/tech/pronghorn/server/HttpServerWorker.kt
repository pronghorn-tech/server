package tech.pronghorn.server

import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.service.Service
import tech.pronghorn.http.HttpResponseHeaderValuePair
import tech.pronghorn.http.protocol.StandardHttpResponseHeaders
import tech.pronghorn.plugins.concurrentSet.ConcurrentSetPlugin
import tech.pronghorn.server.bufferpools.OneUseByteBufferAllocator
import tech.pronghorn.server.bufferpools.ReusableBufferPoolManager
import tech.pronghorn.server.config.HttpServerConfig
import tech.pronghorn.server.handlers.HttpRequestHandler
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

class URLHandlerMapping(val url: ByteArray,
                        val handler: HttpRequestHandler) : ByteBacked {
    override val bytes = url
}

class HttpServerWorker(val server: HttpServer,
                       private val config: HttpServerConfig) : CoroutineWorker() {
    private val connections = ConcurrentSetPlugin.get<HttpServerConnection>()
    private val connectionReadService = ConnectionReadService(this)
    private val connectionCreationService = ServerConnectionCreationService(this, selector)
    private val socketManagerService = server.getSocketManagerService(this, selector)
    private val httpRequestHandlerService = HttpRequestHandlerService(this)
    private val responseWriterService = ResponseWriterService(this)
    private val handlers = HashMap<Int, URLHandlerMapping>()
    private val gmt = ZoneId.of("GMT")
    private val commonHeaderCache = calculateCommonHeaderCache()
    private var latestDate = System.currentTimeMillis() / 1000
    private var handlerFinder: ByteBackedFinder<URLHandlerMapping> = FinderGenerator.generateFinder(handlers.values.toTypedArray())
    val commonHeaderSize = commonHeaderCache.size

    val connectionBufferPool = ReusableBufferPoolManager(config.reusableBufferSize, config.useDirectByteBuffers)
    val oneUseByteBufferAllocator = OneUseByteBufferAllocator(config.useDirectByteBuffers)

    override val services: List<Service> = listOf(
            connectionReadService,
            connectionCreationService,
            socketManagerService,
            httpRequestHandlerService,
            responseWriterService
    )

    private val connectionReadServiceQueueWriter by lazy(LazyThreadSafetyMode.NONE) { connectionReadService.getQueueWriter() }
    private val responseWriterServiceQueueWriter by lazy(LazyThreadSafetyMode.NONE) { responseWriterService.getQueueWriter() }

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
        logger.info { "Worker shutting down ${connections.size} connections" }
        runAllIgnoringExceptions({
            connections.forEach { it.close("Server is shutting down.") }
        })
    }

    private fun calculateCommonHeaderCache(): ByteArray {
        val dateBytes = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(gmt)).toByteArray(Charsets.US_ASCII)
        val dateHeader = HttpResponseHeaderValuePair(StandardHttpResponseHeaders.Date, dateBytes)
        val serverHeader = HttpResponseHeaderValuePair(StandardHttpResponseHeaders.Server, config.serverName)

        val bufferSize = (if (config.sendDateHeader) dateHeader.length else 0) + (if (config.sendServerHeader) serverHeader.length else 0)
        val buffer = ByteBuffer.allocate(bufferSize)

        if (config.sendServerHeader) {
            serverHeader.writeHeader(buffer)
        }
        if (config.sendDateHeader) {
            dateHeader.writeHeader(buffer)
        }

        return Arrays.copyOf(buffer.array(), buffer.capacity())
    }

    fun getCommonHeaders(): ByteArray {
        if (config.sendDateHeader) {
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

    fun getHandler(urlBytes: ByteArray): HttpRequestHandler? = handlerFinder.find(urlBytes)?.handler

    private fun addURLHandler(url: String,
                              handlerGenerator: () -> HttpRequestHandler) {
        val urlBytes = url.toByteArray(Charsets.US_ASCII)
        val handler = handlerGenerator()

        handlers.put(Arrays.hashCode(urlBytes), URLHandlerMapping(urlBytes, handler))
        handlerFinder = FinderGenerator.generateFinder(handlers.values.toTypedArray())
    }

    override fun handleMessage(message: Any): Boolean {
        if (message is RegisterURLHandlerMessage) {
            addURLHandler(message.url, message.handlerGenerator)
            return true
        }

        return false
    }

    override fun processKey(key: SelectionKey) {
        if (key == socketManagerService.acceptSelectionKey && key.isAcceptable) {
            socketManagerService.wake()
        }
        else if (key.isReadable) {
            val attachment = key.attachment()
            if (attachment is HttpServerConnection) {
                if (!attachment.isReadQueued) {
                    if (!connectionReadServiceQueueWriter.offer(attachment)) {
                        logger.warn { "Connection read service is overloaded!" }
                        return
                    }
                    attachment.isReadQueued = true
                }
            }
        }
        else if (key.isWritable) {
            val attachment = key.attachment()
            if (attachment is HttpServerConnection) {
                if (!attachment.isWriteQueued) {
                    if (!responseWriterServiceQueueWriter.offer(attachment)) {
                        logger.warn { "Connection write service is overloaded!" }
                        return
                    }
                    attachment.isWriteQueued = true
                }
            }
        }
        else {
            throw Exception("Unexpected readyOps for attachment : ${key.readyOps()} ${key.attachment()}")
        }
    }
}
