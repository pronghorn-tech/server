package tech.pronghorn.websocket.services

import mu.KotlinLogging
import tech.pronghorn.coroutines.service.InternalQueueService
import tech.pronghorn.http.HttpExchange
import tech.pronghorn.http.HttpRequestParser
import tech.pronghorn.server.HttpServerConnection
import tech.pronghorn.server.HttpWorker
import tech.pronghorn.server.services.HttpRequestHandlerPerRequestService
import java.io.IOException
/*
class WebsocketReadService(override val worker: HttpWorker) : InternalQueueService<HttpServerConnection>() {
    override val logger = KotlinLogging.logger {}
    private val maxFramesParsed = 64

//    private val requestWriter by lazy(LazyThreadSafetyMode.NONE) {
//        worker.requestInternalWriter<HttpExchange, HttpRequestHandlerService>()
//    }

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
*/
