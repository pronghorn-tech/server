/*
 * Copyright 2017 Pronghorn Technology LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.pronghorn.server

import tech.pronghorn.http.*
import tech.pronghorn.http.protocol.parseHttpRequest
import tech.pronghorn.plugins.internalQueue.InternalQueuePlugin
import tech.pronghorn.server.bufferpools.ManagedByteBuffer
import tech.pronghorn.server.requesthandlers.*
import tech.pronghorn.server.selectionhandlers.HttpSocketHandler
import tech.pronghorn.util.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

private val genericNotFoundHandler = StaticHttpRequestHandler(HttpResponses.NotFound())

public open class HttpServerConnection(public val worker: HttpServerWorker,
                                       private val socket: SocketChannel) {
    private var isClosed = false
    private val maxPipelinedRequests = worker.server.config.maxPipelinedRequests
    private val reusableBufferSize = worker.server.config.reusableBufferSize
    private val maxRequestSize = worker.server.config.maxRequestSize
    private val selectionKey = worker.registerSelectionKeyHandler(socket, HttpSocketHandler(this), SelectionKey.OP_READ)
    private var pendingResponse: HttpResponse? = null
    private var remaining = 0L

    private val responsesQueue = InternalQueuePlugin.getBounded<HttpResponse>(maxPipelinedRequests)
    private val requestsQueue = InternalQueuePlugin.getBounded<HttpRequest>(maxPipelinedRequests)

    internal var isReadQueued = false
    internal var isWriteQueued = false
    private var hasPendingWrites = false

    private var readBuffer: ManagedByteBuffer? = null
    private var writeBuffer: ManagedByteBuffer? = null

    private val connectionWriter = worker.responseWriterServiceQueueWriter
    private val requestsReadyWriter = worker.httpRequestHandlerServiceQueueWriter

    private fun releaseReadBuffer() {
        readBuffer?.release()
        readBuffer = null
    }

    private fun releaseWriteBuffer() {
        writeBuffer?.release()
        writeBuffer = null
        if (hasPendingWrites) {
            hasPendingWrites = false
            selectionKey.removeInterestOps(SelectionKey.OP_WRITE)
        }
    }

    private fun getReadBuffer(): ByteBuffer {
        if (readBuffer == null) {
            readBuffer = worker.connectionBufferPool.getBuffer()
        }
        return readBuffer!!.buffer
    }

    private fun getWriteBuffer(): ManagedByteBuffer {
        val writeBuffer = this.writeBuffer
        if (writeBuffer != null) {
            return writeBuffer
        }
        val newBuffer = worker.connectionBufferPool.getBuffer()
        this.writeBuffer = newBuffer
        return newBuffer
    }

    private fun releaseBuffers() {
        releaseReadBuffer()
        releaseWriteBuffer()
    }

    public fun close(reason: String? = null) {
        isReadQueued = false
        isClosed = true
        selectionKey.cancel()
        ignoreExceptions(
                { if (reason != null) socket.write(reason) },
                { socket.close() }
        )
        worker.removeConnection(this)
        releaseBuffers()
    }

    private fun offerResponse(response: HttpResponse): Boolean {
        if (!responsesQueue.offer(response)) {
            close("Maximum pipelining threshold exceeded, pipelining limit: $maxPipelinedRequests")
        }

        if (!isWriteQueued && !hasPendingWrites) {
            isWriteQueued = true
            return connectionWriter.offer(this)
        }
        return true
    }

    internal suspend fun handleRequests() {
        var request = requestsQueue.poll()
        while (request != null) {
            val handler = worker.getHandler(request.url.getPathBytes()) ?: genericNotFoundHandler
            when (handler) {
                is NonSuspendableHttpRequestHandler -> {
                    val response = try {
                        handler.handle(request)
                    }
                    catch (ex: Exception) {
                        HttpResponses.InternalServerError(ex)
                    }

                    if (!offerResponse(response)) {
                        connectionWriter.addAsync(this)
                    }
                }
                is SuspendableHttpRequestHandler -> {
                    // TODO("don't allow pipelining here")
                    worker.launchWorkerCoroutine {
                        val response = try {
                            handler.handle(request)
                        }
                        catch (ex: Exception) {
                            HttpResponses.InternalServerError(ex)
                        }

                        if (!offerResponse(response)) {
                            connectionWriter.addAsync(this)
                        }
                    }
                }
            }
            request = requestsQueue.poll()
        }
    }

    internal suspend fun readRequests() {
        val bytesRead = readIntoBuffer()

        if (bytesRead < 0) {
            close()
        }
        else if (bytesRead > 0) {
            parseRequests()
        }
    }

    private tailrec fun readIntoBuffer(): Int {
        val buffer = getReadBuffer()
        if (!buffer.hasRemaining()) {
            if (buffer.capacity() == maxRequestSize) {
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
            return socket.read(buffer)
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
            var wasEmpty = requestsQueue.isEmpty()
            while (request is HttpRequest) {
                if (!requestsQueue.offer(request)) {
                    close("Maximum pipelining threshold exceeded, pipelining limit: $maxPipelinedRequests")
                }

                if (wasEmpty) {
                    wasEmpty = false
                    if (!requestsReadyWriter.offer(this)) {
                        requestsReadyWriter.addAsync(this)
                        wasEmpty = requestsQueue.isEmpty()
                    }
                }

                if (!buffer.hasRemaining()) {
                    // Recycle empty buffers back into the pool when not in use
                    releaseReadBuffer()
                    return
                }
                preParsePosition = buffer.position()
                request = parseHttpRequest(buffer, this)
            }

            when (request) {
                IncompleteRequestParseError -> {
                    // reset the position so parsing starts at the beginning after more reads
                    buffer.position(preParsePosition)
                    buffer.compact()
                }
                InvalidVersionParseError -> close("Unable to parse HTTP request version.")
                InvalidMethodParseError -> close("Unable to parse HTTP request method.")
                InvalidUrlParseError -> close("Unable to parse HTTP request url.")
                is HttpRequest -> {
                } // no-op
            }
        }
        catch (ex: Exception) {
            ex.printStackTrace()
            close("Unexpected IO exception while reading from socket.")
        }
    }

    private fun flushOrWait(managedBuffer: ManagedByteBuffer?,
                            release: Boolean = false): Boolean {
        if (managedBuffer == null) {
            return true
        }

        val buffer = managedBuffer.buffer
        buffer.flip()

        try {
            val wrote = socket.write(buffer)
            if (wrote < 0) {
                close()
            }
        }
        catch (ex: IOException) {
            close()
        }

        if (!buffer.hasRemaining()) {
            if (release) {
                releaseWriteBuffer()
            }
            else {
                buffer.clear()
            }
            return true
        }
        else {
            buffer.compact()
            this.writeBuffer = managedBuffer
            if (!hasPendingWrites) {
                hasPendingWrites = true
                selectionKey.addInterestOps(SelectionKey.OP_WRITE)
            }
            return false
        }
    }

    private fun getResponse(): HttpResponse? {
        remaining = 0
        return responsesQueue.poll()
    }

    private tailrec fun writeContent(content: ResponseContent,
                                     managed: ManagedByteBuffer? = null): Boolean {
        when (content) {
            is BufferedResponseContent -> {
                val staticManaged = managed ?: getWriteBuffer()
                val buffer = staticManaged.buffer

                if (!buffer.hasRemaining()) {
                    if (flushOrWait(staticManaged)) {
                        return writeContent(content, this.writeBuffer)
                    }
                    else {
                        return false
                    }
                }

                remaining = content.writeToBuffer(buffer, remaining)
                if (remaining > 0) {
                    if (flushOrWait(staticManaged)) {
                        return writeContent(content, this.writeBuffer)
                    }
                    else {
                        return false
                    }
                }
                else {
                    pendingResponse = getResponse()
                    return true
                }
            }
            is DirectToSocketResponseContent -> {
                if (!flushOrWait(this.writeBuffer, true)) {
                    return false
                }

                remaining = content.writeToSocket(socket, remaining)
                if (remaining > 0) {
                    if (!hasPendingWrites) {
                        hasPendingWrites = true
                        selectionKey.addInterestOps(SelectionKey.OP_WRITE)
                    }
                    return false
                }
                else {
                    pendingResponse = getResponse()
                    return true
                }
            }
        }
    }

    internal tailrec fun writeResponses() {
        val nextResponse = pendingResponse ?: getResponse()
        if (nextResponse == null) {
            if (flushOrWait(this.writeBuffer, true) && hasPendingWrites) {
                hasPendingWrites = false
                selectionKey.removeInterestOps(SelectionKey.OP_WRITE)
            }
            return
        }

        if (remaining == 0L) {
            val headerSize = nextResponse.getStatusAndHeaderSize(worker.commonHeaderSize)
            val managedBuffer = getWriteBuffer()
            val buffer = managedBuffer.buffer

            if (buffer.remaining() < headerSize) {
                if (headerSize > buffer.capacity()) {
                    TODO("Might want to enforce this earlier.")
                }

                if (flushOrWait(managedBuffer)) {
                    writeResponses()
                }
            }
            else {
                nextResponse.writeHeadersToBuffer(buffer, worker.getCommonHeaders())
                remaining = nextResponse.content.size
                if (writeContent(nextResponse.content, managedBuffer)) {
                    writeResponses()
                }
                else {
                    pendingResponse = nextResponse
                }
            }
        }
        else {
            if (writeContent(nextResponse.content, this.writeBuffer)) {
                if (hasPendingWrites) {
                    hasPendingWrites = false
                    selectionKey.removeInterestOps(SelectionKey.OP_WRITE)
                }
                writeResponses()
            }
        }
    }
}
