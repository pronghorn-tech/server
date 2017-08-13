package tech.pronghorn.server

import tech.pronghorn.http.HttpResponse
import tech.pronghorn.websocket.core.ParsedHttpRequest
import tech.pronghorn.websocket.core.WebsocketHandshaker
import tech.pronghorn.websocket.protocol.WebsocketFrame
import tech.pronghorn.server.core.HttpRequestHandler
import tech.pronghorn.server.services.HttpRequestHandlerService
import tech.pronghorn.server.services.ResponseWriterService
import com.http.HttpRequest
import mu.KLogger
import mu.KotlinLogging
import tech.pronghorn.coroutines.awaitable.InternalFuture
import tech.pronghorn.coroutines.awaitable.InternalQueue
import tech.pronghorn.coroutines.service.Service
import tech.pronghorn.plugins.spscQueue.SpscQueuePlugin
import tech.pronghorn.util.runAllIgnoringExceptions
import tech.pronghorn.util.write
import tech.pronghorn.server.bufferpools.PooledByteBuffer
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.CancelledKeyException
import java.nio.channels.ClosedChannelException
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.*

const val spaceByte: Byte = 0x20
const val carriageByte: Byte = 0xD
const val returnByte: Byte = 0xA
const val colonByte: Byte = 0x3A
const val tabByte: Byte = 0x9

abstract class HttpConnection(val worker: WebWorker,
                              val socket: SocketChannel,
                              val selectionKey: SelectionKey) {
    companion object {
        private const val responseQueueSize = 64
    }

    private var isClosed = false
    private val logger = KotlinLogging.logger {}
    abstract val shouldSendMasked: Boolean
    abstract val requiresMasked: Boolean
    var isReadQueued = false

    var isHandshakeComplete = false
        private set

    private val connectTime = System.currentTimeMillis()
    private var handshakeBuffer: PooledByteBuffer? = null
    private var readBuffer: PooledByteBuffer? = null
    private var writeBuffer: PooledByteBuffer? = null

    private val readyResponseQueue = SpscQueuePlugin.get<HttpResponse>(responseQueueSize)
    private val readyResponses = InternalQueue(readyResponseQueue)
    private val readyResponseWriter = readyResponses.queueWriter
    private val readyResponseReader = readyResponses.queueReader

    private val connectionWriter by lazy(LazyThreadSafetyMode.NONE) {
        worker.requestInternalWriter<HttpConnection, ResponseWriterService>()
    }

    private val requestsReadyWriter by lazy(LazyThreadSafetyMode.NONE) {
        worker.requestInternalWriter<HttpConnection, HttpRequestHandlerService>()
    }


    init {
        selectionKey.attach(this)
        selectionKey.interestOps(SelectionKey.OP_READ)
    }

    fun releaseReadBuffer() {
        readBuffer?.release()
        readBuffer = null
    }

    fun releaseWriteBuffer() {
        writeBuffer?.release()
        writeBuffer = null
    }

    fun releaseHandshakeBuffer() {
        handshakeBuffer?.release()
        handshakeBuffer = null
    }

    fun removeInterestOps(removeInterestOps: Int) {
        try {
            selectionKey.interestOps(selectionKey.interestOps() and removeInterestOps.inv())
        } catch (ex: CancelledKeyException) {
            close("Connection closed.")
        }
    }

    fun addInterestOps(newInterestOps: Int) {
        try {
            selectionKey.interestOps(selectionKey.interestOps() or newInterestOps)
        } catch (ex: CancelledKeyException) {
            close("Connection closed.")
        }
    }

    fun updateInterestOps(newInterestOps: Int) {
        try {
            selectionKey.interestOps(newInterestOps)
        } catch (ex: CancelledKeyException) {
            close("Connection closed.")
        }
    }

    private fun getHandshakeBuffer(): ByteBuffer {
        if (handshakeBuffer == null) {
            handshakeBuffer = worker.handshakeBufferPool.getBuffer()
        }
        return handshakeBuffer!!.buffer
    }

    fun getReadBuffer(): ByteBuffer {
        if (readBuffer == null) {
            readBuffer = worker.connectionBufferPool.getBuffer()
        }
        return readBuffer!!.buffer
    }

    fun getWriteBuffer(): ByteBuffer {
        if (writeBuffer == null) {
            writeBuffer = worker.connectionBufferPool.getBuffer()
        }
        return writeBuffer!!.buffer
    }

    private fun releaseBuffers() {
        releaseHandshakeBuffer()
        releaseReadBuffer()
        releaseWriteBuffer()
    }

    open fun close(reason: String? = null) {
        logger.debug { "Closing connection : $reason" }
        isReadQueued = false
        isClosed = true
        selectionKey.cancel()
        runAllIgnoringExceptions(
                { if (reason != null) socket.write(reason) },
                { socket.close() }
        )
        worker.removeConnection(this)
        releaseBuffers()
    }

    fun connectionTime(): Long {
        return System.currentTimeMillis() - connectTime
    }

    private fun parseHandshakeRequest(): ParsedHttpRequest? {
        TODO()
//        if (!socket.isOpen) {
//            return null
//        }
//
//        val buffer = getHandshakeBuffer()
//        val read = socket.read(buffer)
//        if (read < 0) {
//            close("Disconnected.")
//            return null
//        }
//
//        if (read > 0) {
//            buffer.flip()
//            val request = HttpRequestParser.parse(buffer)
//            if (request == null) {
//                if (buffer.limit() == buffer.capacity()) {
//                    // The entire handshake buffer is full, but still no valid handshake
//                    close("Websocket handshake not received before data or too large.")
//                    return null
//                } else {
//                    // Handshake assumed to be partially read, reset the buffer for more reading
//                    buffer.position(buffer.limit())
//                    buffer.limit(buffer.capacity())
//                    // Set the appropriate interestOps for more reading
//                    selectionKey.interestOps(selectionKey.interestOps() or SelectionKey.OP_READ)
//                    return null
//                }
//            } else {
//                releaseHandshakeBuffer()
//                return request
//            }
//        }

        return null
    }

    abstract fun handleHandshakeRequest(request: ParsedHttpRequest,
                                        handshaker: WebsocketHandshaker): Boolean


    fun attemptHandshake(handshaker: WebsocketHandshaker) {
        val request = parseHandshakeRequest()
        if (request != null) {
            // With a request, handleHandshake either succeeds, or closes the connection
            val success = handleHandshakeRequest(request, handshaker)
            if (success) {
                isHandshakeComplete = true
                selectionKey.interestOps(selectionKey.interestOps() or SelectionKey.OP_READ)
                worker.clearPendingConnection(this)
            }
        }
    }

    suspend fun appendResponse(response: HttpResponse) {
        val empty = readyResponseReader.isEmpty()
        // TODO: is this better than just the addAsync?
        if(!readyResponseWriter.offer(response)) {
            readyResponseWriter.addAsync(response)
        }

        if (empty) {
            // TODO: is this better than just the addAsync?
            if(!connectionWriter.offer(this)) {
                connectionWriter.addAsync(this)
            }
        }
    }

    private val queuedRequestsQueue = SpscQueuePlugin.get<HttpRequest>(1024)
    private val queuedRequests = InternalQueue(queuedRequestsQueue)
    private val queuedRequestsWriter = queuedRequests.queueWriter
    private val queuedRequestsReader = queuedRequests.queueReader

    suspend fun queueRequest(request: HttpRequest) {
        if (queuedRequestsReader.isEmpty()) {
            requestsReadyWriter.addAsync(this)
        }
        queuedRequestsWriter.addAsync(request)
    }

    suspend fun handleRequests(requestHandler: HttpRequestHandler) {
        var request = queuedRequestsReader.poll()
        while (request != null) {
            val response = requestHandler.handleRequest(request)
            appendResponse(response)
            request = queuedRequestsReader.poll()
        }
    }

    fun writeResponse(response: HttpResponse): Boolean {
        if (isClosed) {
            return true
        }

        val buffer = getWriteBuffer()
        renderResponseDirect(buffer, response)
        buffer.flip()
        try {
            socket.write(buffer)
        } catch (ex: ClosedChannelException) {
            close()
            return true
        }
        if (!buffer.hasRemaining()) {
            releaseWriteBuffer()
            return true
        } else {
            return false
        }
    }

    fun writeResponses(): Boolean {
        val buffer = getWriteBuffer()
        if (!buffer.hasRemaining()) {
            logger.error("FULL BUFFER")
            TODO()
            val wrote = socket.write(buffer)
            if (wrote < 0) {
                logger.error("DISCONNECTED DURING WRITE")
                return true
            } else if (wrote == 0) {
                logger.error("WROTE 0 BYTES")
                return false
            } else {
                logger.info("Wrote $wrote bytes")
                if (!buffer.hasRemaining()) {
                    return true
                }
            }
        } else {
            var response = readyResponseReader.poll()
            while (buffer.hasRemaining() && response != null) {
                renderResponseDirect(buffer, response)
                response = readyResponseReader.poll()
            }
            buffer.flip()
            val wrote = socket.write(buffer)
            logger.debug { "Wrote $wrote bytes to socket." }
            //logger.error("buffer after write: position: ${buffer.position()}, limit: ${buffer.limit()}, remaining: ${buffer.remaining()}, hasRemaining: ${buffer.hasRemaining()}")
//            println("Wrote $wrote bytes")
            if (!buffer.hasRemaining()) {
                releaseWriteBuffer()
                return true
            } else {
//                logger.error("INCOMPLETE WRITE")
                return false
//                System.exit(1)
            }
        }
    }

    fun renderResponseDirect(buffer: ByteBuffer,
                             response: HttpResponse): Boolean {
        //val dateBytes = worker.getDateHeaderValue()
        val size = response.getOutputSize()
        //val start = buffer.position()

        if (buffer.remaining() < size) {
            return false
        }

        buffer.put(response.httpVersion.bytes)
        buffer.put(spaceByte)
        buffer.put(response.code.bytes)
        buffer.put(carriageByte)
        buffer.put(returnByte)

        response.headers.forEach { header ->
            header.writeHeaderDirect(buffer, buffer.position())
        }

        buffer.put(carriageByte)
        buffer.put(returnByte)

        if (response.body.isNotEmpty()) {
            buffer.put(response.body, 0, response.body.size)
        }

        return true
    }

    fun renderResponse(buffer: ByteBuffer,
                       response: HttpResponse): Boolean {
        val dateBytes = worker.getDateHeaderValue()
        val size = response.getOutputSize()

        if (buffer.remaining() < size) {
            return false
        }

        val offset = buffer.position()
        val output = buffer.array()

        var z = offset

        System.arraycopy(response.httpVersion.bytes, 0, output, z, response.httpVersion.bytes.size)
        z += response.httpVersion.bytes.size
        output[z] = spaceByte
        z += 1

        System.arraycopy(response.code.bytes, 0, output, z, response.code.bytes.size)
        z += response.code.bytes.size
        output[z] = carriageByte
        output[z + 1] = returnByte
        z += 2

//        z += writeNumericHeader(HttpResponseHeader.ContentLength, response.body.size, output, z)
//        z += writeHeader(HttpResponseHeader.Server, response.serverBytes, output, z)
//        z += writeHeader(HttpResponseHeader.Date, dateBytes, output, z)

        var x = 0
        while (x < response.headers.size) {
            z += response.headers[x].writeHeader(output, z)
            x += 1
        }
//        response.headers.forEach { header ->
//            z += header.writeHeader(output, z)
//        }

//        response.headers.forEach { header ->
//            z += writeHeader(header.key, header.value, output, z)
//        }

        output[z] = carriageByte
        output[z + 1] = returnByte
        z += 2

        if (response.body.isNotEmpty()) {
            System.arraycopy(response.body, 0, output, z, response.body.size)
        }

        z += response.body.size

        buffer.position(z)
        return true
    }

//    protected fun finalize() {
//        if (socket.isOpen || socket.isConnected) {
//            println("FAILED TO CLOSE SOCKET HttpConnection")
//            println("${socket.isOpen} / ${socket.isConnected}")
//            System.exit(1)
//        }
//        else if (readBuffer != null || writeBuffer != null || handshakeBuffer != null) {
//            println("FAILED TO RELEASE BUFFER HttpConnection")
//            System.exit(1)
//        }
//    }
}

class HttpServerConnection(worker: WebServerWorker,
                           socket: SocketChannel,
                           selectionKey: SelectionKey) : HttpConnection(worker, socket, selectionKey) {
    override val shouldSendMasked: Boolean = false
    override val requiresMasked: Boolean = true

    override fun handleHandshakeRequest(request: ParsedHttpRequest,
                                        handshaker: WebsocketHandshaker): Boolean {
        val headers = request.headers
        val key = headers["Sec-WebSocket-Key"]
        if (key == null) {
            close("Websocket handshakes must include a Sec-WebSocket-Key header.")
            return false
        } else {
            val handshake = handshaker.getServerHandshakeResponse(key)
            try {
                socket.write(handshake)
                return true
            } catch (e: IOException) {
                close("Unexpected error replying to initial handshake.")
                return false
            }
        }
    }
}

class HttpClientConnection(worker: WebClientWorker,
                           socket: SocketChannel,
                           selectionKey: SelectionKey,
                           private val readyPromise: InternalFuture.InternalPromise<HttpClientConnection>) : HttpConnection(worker, socket, selectionKey) {
    override val shouldSendMasked: Boolean = false
    override val requiresMasked: Boolean = true
    private val sendQueue = SpscQueuePlugin.get<WebsocketFrame>(1024)

    override fun close(reason: String?) {
        if (!isHandshakeComplete) {
            readyPromise.completeExceptionally(ConnectException(reason))
        }
        super.close(reason)
    }

//    fun getFrameWriter(): QueueWriter<WebsocketFrame> {
//        TODO()
    //ExternalQueueWriter<WebsocketFrame>(sendQueue,
//    }

    suspend fun send(frame: WebsocketFrame): Unit {
        if (!sendQueue.offer(frame)) {
            TODO()
        }
    }

    fun sendHandshake(handshaker: WebsocketHandshaker,
                      randomGenerator: Random) {
        val address = socket.remoteAddress
        if (address is InetSocketAddress) {
            val keyBytes = ByteArray(16)
            randomGenerator.nextBytes(keyBytes)
            val handshake = handshaker.getClientHandshakeRequest(address.hostName, keyBytes)
            try {
                socket.write(handshake)
            } catch (e: IOException) {
                close("Unexpected error replying to initial handshake.")
            }
        } else {
            throw Exception("Unexpected socket address, should be InetSocketAddress")
        }
    }

    override fun handleHandshakeRequest(request: ParsedHttpRequest,
                                        handshaker: WebsocketHandshaker): Boolean {
        // TODO: validate the server sent some sensible handshake response
        readyPromise.complete(this)
        return true
    }
}

class DummyConnection(worker: WebWorker,
                      socket: SocketChannel,
                      selectionKey: SelectionKey) : HttpConnection(worker, socket, selectionKey) {

    override fun handleHandshakeRequest(request: ParsedHttpRequest, handshaker: WebsocketHandshaker): Boolean = false

    override val shouldSendMasked: Boolean = false
    override val requiresMasked: Boolean = false

}
