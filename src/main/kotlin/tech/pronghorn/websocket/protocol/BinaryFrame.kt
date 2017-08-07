package tech.pronghorn.websocket.protocol

import tech.pronghorn.server.HttpConnection

class BinaryFrame(override val payload: ByteArray,
                  override val connection: HttpConnection) : WebsocketFrame(FrameType.BINARY) {
    override fun toString(): String {
        return "BinaryFrame(byte[" + payload.size + "])"
    }
}
