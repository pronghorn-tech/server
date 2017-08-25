package tech.pronghorn.server.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.http.HttpRequest
import tech.pronghorn.http.HttpRequestParser
import tech.pronghorn.server.HttpServerConnection
import tech.pronghorn.server.HttpWorker
import java.io.IOException
import java.nio.channels.SelectionKey

class ConnectionReadService(override val worker: HttpWorker) : InternalQueueService<HttpServerConnection>() {
    override val logger = KotlinLogging.logger {}
    private val maxFramesParsed = 64
//    private var connectionsProcessed = 0

//    private val requestWriter by lazy(LazyThreadSafetyMode.NONE) {
//        worker.requestInternalWriter<HttpRequest, HttpRequestHandlerService>()
//    }

    private val requestWriter by lazy(LazyThreadSafetyMode.NONE) {
        worker.requestInternalWriter<HttpRequest, HttpRequestHandlerPerRequestService>()
    }

//    private val frameWriter by lazy(LazyThreadSafetyMode.NONE) {
//        worker.requestInternalWriter<WebsocketFrame, FrameHandlerService>()
//    }

/*
    override fun shouldYield(): Boolean {
//        if(connectionsProcessed > 64) {
//            connectionsProcessed = 0
//            return true
//        }
//        else {
            return false
//        }
    }
*/

    override suspend fun process(connection: HttpServerConnection): Boolean {
//        connectionsProcessed += 1
//        val preRead = System.currentTimeMillis()
        val bytesRead = connection.readIntoBuffer()
//        val postRead = System.currentTimeMillis()
//        if(postRead - preRead > 10){
//            logger.error("TOOK ${postRead - preRead} ms to do a read!?")
//        }

        if(bytesRead < 0){
            // The other end of the socket has disconnected, end processing immediately
            connection.close("Disconnected.")
            return true
        }
        else if(bytesRead == 0){
            return true
        }

//        val preParse = System.currentTimeMillis()
        connection.parseRequests(maxFramesParsed)
//        val postParse = System.currentTimeMillis()
//        if(postParse - preParse > 10){
//            logger.error("TOOK ${postParse - preParse} ms to parse a request!?")
//        }

        if(connection.getReadBuffer().position() == 0){
            // Recycle empty buffers back into the pool when not in use
            connection.releaseReadBuffer()
        }

        connection.isReadQueued = false
//            connection.addInterestOps(SelectionKey.OP_READ)
        return true
    }

    suspend fun processOLD(connection: HttpServerConnection): Boolean {
//        connectionsProcessed += 1
        var totalRequestsParsed = 0
        var bytesRead = connection.readIntoBuffer()
        if(bytesRead < 0){
            // The other end of the socket has disconnected, end processing immediately
            connection.close("Disconnected.")
            return true
        }
        else if(bytesRead == 0){
            return true
        }

        var requestsParsed = connection.parseRequests(maxFramesParsed - totalRequestsParsed)
        while (true) {
            totalRequestsParsed += requestsParsed

            if(bytesRead == 0 && requestsParsed == 0){
                // This connection has no more frames ready
                break
            }

            if (totalRequestsParsed >= maxFramesParsed) {
                break
            }

            bytesRead = connection.readIntoBuffer()
            if(bytesRead < 0){
                // The other end of the socket has disconnected, end processing immediately
                connection.close("Disconnected.")
                return true
            }
            logger.debug { "Read $bytesRead bytes into buffer from connection." }
            requestsParsed = connection.parseRequests(maxFramesParsed - totalRequestsParsed)
        }

        logger.debug { "Parsed $totalRequestsParsed new requests." }

        if(connection.getReadBuffer().position() == 0){
            // Recycle empty buffers back into the pool when not in use
            connection.releaseReadBuffer()
        }

        if(bytesRead > 0 || requestsParsed > 0){
            return false
        }
        else {
            connection.isReadQueued = false
//            connection.addInterestOps(SelectionKey.OP_READ)
            return true
        }
    }

    /*
    suspend fun processWEBSOCKET(connection: HttpServerConnection): Boolean {
//        connectionsProcessed += 1
        var totalFramesParsed = 0
        var bytesRead = connection.readIntoBuffer()
        if(bytesRead < 0){
            // The other end of the socket has disconnected, end processing immediately
            connection.close("Disconnected.")
            return true
        }

        var framesParsed = connection.parseFrames(maxFramesParsed - totalFramesParsed)
        while (true) {
            totalFramesParsed += framesParsed

            if(bytesRead == 0 && framesParsed == 0){
                // This connection has no more frames ready
                break
            }

            if (totalFramesParsed >= maxFramesParsed) {
                break
            }

            bytesRead = connection.readIntoBuffer()
            if(bytesRead < 0){
                // The other end of the socket has disconnected, end processing immediately
                connection.close("Disconnected.")
                return true
            }
            framesParsed = connection.parseFrames(maxFramesParsed - totalFramesParsed)
        }

        if(connection.getReadBuffer().position() == 0){
            // Recycle empty buffers back into the pool when not in use
            connection.releaseReadBuffer()
        }

        if(bytesRead > 0 || framesParsed > 0){
            return false
        }
        else {
            connection.addInterestOps(SelectionKey.OP_READ)
            return true
        }
    }
    */

    private fun HttpServerConnection.readIntoBuffer(): Int {
        val buffer = getReadBuffer()
        if (!buffer.hasRemaining()) {
            // buffer is already full
            return 0
        }

        var totalRead = 0
        try {
//            val preRead = System.currentTimeMillis()
            var readBytes = socket.read(buffer)
//            val postRead = System.currentTimeMillis()
//            if(postRead - preRead > 10){
//                logger.error("REALLY TOOK ${postRead - preRead} ms to do a read ${readBytes} bytes!?")
//            }

//            while (readBytes > 0) {
                totalRead += readBytes
//                if (!buffer.hasRemaining()) {
//                    break
//                }
//
//                readBytes = socket.read(buffer)
//            }
//
//            if(readBytes < 0){
//                // disconnects and end processing immediately
//                logger.warn("Connection closed.")
//                return readBytes
//            }
        }
        catch (ex: IOException) {
            close("Unexpected IO Exception")
        }

        return totalRead
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
            // TODO: handle certain parse errors properly here.
            while (request is HttpRequest) {
//                requestWriter.addAsync(request) // Faster for non-pipelining
                queueRequest(request) // Faster for pipelining
                requestsParsed += 1
                if (!buffer.hasRemaining() || requestsParsed >= maxToParse) {
                    break
                }
                request = HttpRequestParser.parse(buffer, this)
                // TODO: handle certain parse errors properly here.
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

    /*
    private suspend fun HttpServerConnection.parseFrames(maxToParse: Int): Int {
        val buffer = getReadBuffer()
        if (buffer.position() == 0) {
            return 0
        }

        var framesParsed = 0
        buffer.flip()

        try {
            var frame = FrameParser.parseFrame(buffer, this)
            while (frame != null) {
                frameWriter.addAsync(frame)
                framesParsed += 1
                if (framesParsed >= maxToParse) {
                    break
                }
                frame = FrameParser.parseFrame(buffer, this)
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

        return framesParsed
    }
    */
}
