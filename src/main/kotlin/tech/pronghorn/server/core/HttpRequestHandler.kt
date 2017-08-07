package tech.pronghorn.server.core

import com.http.HttpRequest
import tech.pronghorn.http.HttpResponse
import com.http.protocol.HttpMethod

abstract class HttpRequestHandler {
    internal suspend fun handleRequest(request: HttpRequest): HttpResponse {
        return when (request.method) {
            HttpMethod.GET -> handleGet(request)
//            FrameType.PING -> handlePingFrame(frame as PingFrame)
//            FrameType.PONG -> handlePongFrame(frame as PongFrame)
//            FrameType.CLOSE -> handleCloseFrame(frame as CloseFrame)
//            FrameType.BINARY -> handleBinaryFrame(frame as BinaryFrame)
//            FrameType.CONTINUATION -> TODO()
            else -> TODO()
        }
    }

    protected abstract suspend fun handleGet(request: HttpRequest): HttpResponse
//
//    protected abstract suspend fun handlePingFrame(frame: PingFrame): Unit
//
//    protected abstract suspend fun handlePongFrame(frame: PongFrame): Unit
//
//    protected abstract suspend fun handleCloseFrame(frame: CloseFrame): Unit
//
//    protected abstract suspend fun handleBinaryFrame(frame: BinaryFrame): Unit
}
