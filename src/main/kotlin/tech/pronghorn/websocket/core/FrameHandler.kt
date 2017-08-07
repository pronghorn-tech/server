package tech.pronghorn.websocket.core

import tech.pronghorn.websocket.protocol.*

/**
 * Defines actual processing to be done on incoming WebsocketFrames
 */
abstract class FrameHandler {
    internal suspend fun handleFrame(frame: WebsocketFrame) {
        when (frame.frameType) {
            FrameType.TEXT -> handleTextFrame(frame as TextFrame)
            FrameType.PING -> handlePingFrame(frame as PingFrame)
            FrameType.PONG -> handlePongFrame(frame as PongFrame)
            FrameType.CLOSE -> handleCloseFrame(frame as CloseFrame)
            FrameType.BINARY -> handleBinaryFrame(frame as BinaryFrame)
            FrameType.CONTINUATION -> TODO()
        }
    }

    protected abstract suspend fun handleTextFrame(frame: TextFrame): Unit

    protected abstract suspend fun handlePingFrame(frame: PingFrame): Unit

    protected abstract suspend fun handlePongFrame(frame: PongFrame): Unit

    protected abstract suspend fun handleCloseFrame(frame: CloseFrame): Unit

    protected abstract suspend fun handleBinaryFrame(frame: BinaryFrame): Unit
}

