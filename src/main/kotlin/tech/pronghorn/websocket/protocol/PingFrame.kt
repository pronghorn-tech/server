package tech.pronghorn.websocket.protocol

import tech.pronghorn.server.HttpConnection

class PingFrame(override val payload: ByteArray,
                override val connection: HttpConnection) : WebsocketFrame(FrameType.PING) {

    override fun toString(): String {
        return "PingFrame()"
    }
}
