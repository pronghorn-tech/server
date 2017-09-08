package tech.pronghorn.server

import mu.KotlinLogging
import tech.pronghorn.http.*
import tech.pronghorn.plugins.internalQueue.InternalQueuePlugin
import tech.pronghorn.server.bufferpools.ManagedByteBuffer
import tech.pronghorn.server.handlers.StaticHttpRequestHandler
import tech.pronghorn.server.services.HttpRequestHandlerService
import tech.pronghorn.server.services.ResponseWriterService
import tech.pronghorn.util.runAllIgnoringExceptions
import tech.pronghorn.util.write
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.CancelledKeyException
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

private val genericNotFoundHandler = StaticHttpRequestHandler(HttpResponses.NotFound())

open class HttpServerConnection(val worker: HttpServerWorker,
                                val socket: SocketChannel,
                                val selectionKey: SelectionKey) {
    private var isClosed = false
    private val logger = KotlinLogging.logger {}
    private val maxPipelinedRequests = worker.server.config.maxPipelinedRequests
    private val reusableBufferSize = worker.server.config.reusableBufferSize
    private val maxRequestSize = worker.server.config.maxRequestSize

    private val responsesQueue = InternalQueuePlugin.get<HttpResponse>(maxPipelinedRequests)
    private val requestsQueue = InternalQueuePlugin.get<HttpExchange>(maxPipelinedRequests)

    var isReadQueued = false
    var isWriteQueued = false
    var hasPendingWrites = false

    private var readBuffer: ManagedByteBuffer? = null
    private var writeBuffer: ManagedByteBuffer? = null

    private val connectionWriter by lazy(LazyThreadSafetyMode.NONE) {
        worker.requestInternalWriter<HttpServerConnection, ResponseWriterService>()
    }

    private val requestsReadyWriter by lazy(LazyThreadSafetyMode.NONE) {
        worker.requestInternalWriter<HttpServerConnection, HttpRequestHandlerService>()
    }

    init {
        selectionKey.attach(this)
        selectionKey.interestOps(SelectionKey.OP_READ)
    }

    private fun releaseReadBuffer() {
        readBuffer?.release()
        readBuffer = null
    }

    private fun releaseWriteBuffer() {
        writeBuffer?.release()
        writeBuffer = null
        if (hasPendingWrites) {
            hasPendingWrites = false
            removeInterestOps(SelectionKey.OP_WRITE)
        }
    }

    private fun getReadBuffer(): ByteBuffer {
        if (readBuffer == null) {
            readBuffer = worker.connectionBufferPool.getBuffer()
        }
        return readBuffer!!.buffer
    }

    private fun releaseBuffers() {
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

    suspend fun enqueueResponse(response: HttpResponse) {
        if (!responsesQueue.offer(response)) {
            close("Maximum pipelining threshold exceeded, pipelining limit: $maxPipelinedRequests")
        }

        if (!isWriteQueued && !hasPendingWrites) {
            isWriteQueued = true
            connectionWriter.addAsync(this)
        }
    }

    suspend fun enqueueRequest(exchange: HttpExchange) {
        val wasEmpty = requestsQueue.isEmpty()
        requestsQueue.add(exchange)
        if (wasEmpty) {
            requestsReadyWriter.addAsync(this)
        }
    }

    suspend fun handleRequests() {
        var request = requestsQueue.poll()
        while (request != null) {
            val handler = worker.getHandler(request.requestUrl.getPathBytes()) ?: genericNotFoundHandler
            handler.handle(request)
            request = requestsQueue.poll()
        }
    }

    suspend fun readRequests(){
        val bytesRead = readIntoBuffer()

        if(bytesRead < 0){
            close()
        }
        else if(bytesRead > 0) {
            parseRequests()
        }
    }

    private tailrec fun readIntoBuffer(): Int {
        val buffer = getReadBuffer()
        if (!buffer.hasRemaining()) {
            if(buffer.capacity() == maxRequestSize){
                close("Request too large.")
                return 0
            }
            val oneUseBuffer = worker.oneUseByteBufferAllocator.getBuffer(Math.min(maxRequestSize, buffer.capacity() * 2))
            oneUseBuffer.buffer.put(buffer)
            releaseReadBuffer()
            readBuffer = oneUseBuffer
            return readIntoBuffer()
        }

        try {
            val readBytes = socket.read(buffer)
            return readBytes
        }
        catch (ex: IOException) {
            close("Unexpected IO Exception")
            return 0
        }
    }

    private suspend fun parseRequests() {
        val buffer = getReadBuffer()
        buffer.flip()

        try {
            var preParsePosition = buffer.position()
            var request = parseHttpRequest(buffer, this)

            while (request is HttpExchange) {
                enqueueRequest(request)
                if(!buffer.hasRemaining()){
                    // Recycle empty buffers back into the pool when not in use
                    releaseReadBuffer()
                    return
                }
                preParsePosition = buffer.position()
                request = parseHttpRequest(buffer, this)
            }

            when(request) {
                IncompleteRequestParseError -> {
                    // reset the position so parsing starts at the beginning after more reads
                    buffer.position(preParsePosition)
                    buffer.compact()
                }
                InvalidVersionParseError -> close("Unable to parse HTTP request version.")
                InvalidMethodParseError -> close("Unable to parse HTTP request method.")
                InvalidUrlParseError -> close("Unable to parse HTTP request url.")
                InsecureCredentialsParseError -> close("Credentials provided on unsecure connection.")
                is HttpExchange -> {} // no-op
            }
        }
        catch (ex: Exception) {
            ex.printStackTrace()
            close("Unexpected IO exception while reading from socket.")
        }
    }

    private fun flushAndReleaseOrWait(managedBuffer: ManagedByteBuffer?): Boolean {
        if (managedBuffer == null) {
            return true
        }

        if (!hasPendingWrites) {
            managedBuffer.buffer.flip()
        }

        try {
            val wrote = socket.write(managedBuffer.buffer)
            if (wrote < 0) {
                close()
            }
        }
        catch (ex: IOException) {
            close()
        }

        if (!managedBuffer.buffer.hasRemaining()) {
            releaseWriteBuffer()
            return true
        }
        else {
            this.writeBuffer = managedBuffer
            if (!hasPendingWrites) {
                hasPendingWrites = true
                addInterestOps(SelectionKey.OP_WRITE)
            }
            return false
        }
    }

    tailrec fun writeResponses() {
        val nextResponse = responsesQueue.peek()
        if (nextResponse == null) {
            flushAndReleaseOrWait(this.writeBuffer)
        }
        else {
            val nextResponseSize = nextResponse.getOutputSize(worker.commonHeaderSize)
            val currentManagedBuffer = this.writeBuffer

            if (currentManagedBuffer == null) {
                val newManagedBuffer = if (nextResponseSize < reusableBufferSize) {
                    worker.connectionBufferPool.getBuffer()
                }
                else {
                    worker.oneUseByteBufferAllocator.getBuffer(nextResponseSize)
                }

                this.writeBuffer = newManagedBuffer
                responsesQueue.remove().writeToBuffer(newManagedBuffer.buffer, worker.getCommonHeaders())
                writeResponses()
            }
            else {
                val currentBuffer = currentManagedBuffer.buffer

                if (hasPendingWrites) {
                    if (nextResponseSize <= currentBuffer.capacity() - currentBuffer.limit()) {
                        currentBuffer.position(currentBuffer.limit())
                        currentBuffer.limit(currentBuffer.capacity())

                        responsesQueue.remove().writeToBuffer(currentBuffer, worker.getCommonHeaders())
                        writeResponses()
                    }
                    else if (nextResponseSize <= currentBuffer.position() + (currentBuffer.capacity() - currentBuffer.limit())) {
                        currentBuffer.compact()

                        responsesQueue.remove().writeToBuffer(currentBuffer, worker.getCommonHeaders())
                        writeResponses()
                    }
                    else {
                        if (flushAndReleaseOrWait(currentManagedBuffer)) {
                            writeResponses()
                        }
                    }
                }
                else if (nextResponseSize <= currentBuffer.remaining()) {
                    responsesQueue.remove().writeToBuffer(currentBuffer, worker.getCommonHeaders())
                    writeResponses()
                }
                else {
                    if (flushAndReleaseOrWait(currentManagedBuffer)) {
                        writeResponses()
                    }
                }
            }
        }
    }

    private fun removeInterestOps(removeInterestOps: Int) {
        try {
            selectionKey.interestOps(selectionKey.interestOps() and removeInterestOps.inv())
        }
        catch (ex: CancelledKeyException) {
            close("Connection closed.")
        }
    }

    private fun addInterestOps(newInterestOps: Int) {
        try {
            selectionKey.interestOps(selectionKey.interestOps() or newInterestOps)
        }
        catch (ex: CancelledKeyException) {
            close("Connection closed.")
        }
    }
}
