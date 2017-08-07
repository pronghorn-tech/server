package tech.pronghorn.websocket.protocol

import tech.pronghorn.server.HttpConnection

abstract class WebsocketFrame internal constructor(val frameType: FrameType) {
    abstract val payload: ByteArray
    abstract val connection: HttpConnection

    fun getEncodedLength(masked: Boolean): Int {
        return payload.size + if (masked) 6 else 2
    }

    companion object {
        const val MAGIC_NUMBER = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
    }
}

