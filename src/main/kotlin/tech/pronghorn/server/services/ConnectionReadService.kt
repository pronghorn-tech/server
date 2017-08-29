package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.http.*
import tech.pronghorn.server.HttpServerConnection
import tech.pronghorn.server.HttpWorker
import java.io.IOException

class ConnectionReadService(override val worker: HttpWorker) : InternalQueueService<HttpServerConnection>() {
    override val logger = KotlinLogging.logger {}
    private val maxFramesParsed = 64

    private val requestWriter by lazy(LazyThreadSafetyMode.NONE) {
        worker.requestInternalWriter<HttpExchange, HttpRequestHandlerPerRequestService>()
    }

    override suspend fun process(connection: HttpServerConnection): Boolean {
        val bytesRead = connection.readIntoBuffer()
        if (bytesRead < 0) {
            // The other end of the socket has disconnected, end processing immediately
            connection.close("Disconnected.")
            return true
        }
        else if (bytesRead == 0) {
            return true
        }

        connection.parseRequests(maxFramesParsed)

        if (connection.getReadBuffer().position() == 0) {
            // Recycle empty buffers back into the pool when not in use
            connection.releaseReadBuffer()
        }

        connection.isReadQueued = false
        return true
    }

    private fun HttpServerConnection.readIntoBuffer(): Int {
        val buffer = getReadBuffer()
        if (!buffer.hasRemaining()) {
            // buffer is already full
            return 0
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

    private suspend fun HttpServerConnection.parseRequests(maxToParse: Int): Int {
        val buffer = getReadBuffer()
        if (!buffer.hasRemaining()) {
            return 0
        }

        var requestsParsed = 0
        buffer.flip()

        try {
            var request = HttpRequestParser.parse(buffer, this)
            while (request is HttpExchange) {
//                requestWriter.addAsync(request) // Faster for non-pipelining
                queueRequest(request) // Faster for pipelining
                requestsParsed += 1
                if (!buffer.hasRemaining() || requestsParsed >= maxToParse) {
                    break
                }
                request = HttpRequestParser.parse(buffer, this)
            }

            when(request) {
                is InvalidVersionParseError -> TODO()
                is InvalidMethodParseError -> TODO()
                is IncompleteRequestParseError -> TODO()
            }
        }
        catch (ex: Exception) {
            ex.printStackTrace()
            close("Unexpected IO exception while reading from socket.")
        }
        finally {
            if (buffer.remaining() == 0) {
                buffer.clear()
            }
            else {
                buffer.compact()
            }
        }

        return requestsParsed
    }
}
