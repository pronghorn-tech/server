package tech.pronghorn.websocket.protocol

import tech.pronghorn.server.HttpConnection
import java.util.*

class CloseFrame(override val payload: ByteArray,
                 override val connection: HttpConnection) : WebsocketFrame(FrameType.CLOSE) {
    override fun toString(): String {
        return "CloseFrame($statusCode, $reason)"
    }

    val reason: String
        get() = if (payload.size < 3) "" else String(Arrays.copyOfRange(payload, 2, payload.size))

    val statusCode: Short
        get() = if (payload.size < 2) 0.toShort() else (payload[0].toInt() shl 8 or payload[1].toInt()).toShort()
}
