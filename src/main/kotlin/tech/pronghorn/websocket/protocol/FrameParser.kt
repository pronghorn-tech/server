package tech.pronghorn.websocket.protocol

import tech.pronghorn.server.HttpConnection
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.experimental.xor

object FrameParser {
    fun parseFrame(buffer: ByteBuffer,
                   connection: HttpConnection): WebsocketFrame? {
        if (buffer.remaining() < 2) {
            return null
        }

        val offset = buffer.position()

        val secondByte = buffer.get(offset + 1)
        val isMasked = secondByte.toInt() shr 8 and 1 == 1

        if (connection.requiresMasked && !isMasked) {
            // TODO: throw some more sensible exception type
            throw Exception("Expected masked data from client.")
        }

        var dataStart = 2
        var payloadLength = (secondByte and 127).toLong()
        if (payloadLength == 126L) {
            // Have to read the length in two byte long pieces due to Java's lack of unsigned shorts
            payloadLength = (buffer.get(offset + 2).toInt() shl 8 + buffer.get(offset + 3).toInt()).toLong()
            dataStart = 4
        } else if (payloadLength == 127L) {
            payloadLength = buffer.getLong(offset + 2)
            dataStart = 10
        }

        if (payloadLength > 1024 * 1024) {
            // TODO: this should be slightly less than a MB, frame headers push the max message size possible below this
            throw Exception("Websocket payloads greater than 1MB are not allowed.")
        }

        val totalMessageLength = dataStart.toLong() + payloadLength + (if (isMasked) 4 else 0).toLong()

        // Short circuit here if the message is incomplete
        if (buffer.limit() - offset < totalMessageLength) {
            return null
        }

        val firstByte = buffer.get(offset)
        val fin = firstByte.toInt() shr 8 and 1 == 1
        if (!fin) {
            throw Exception("Continuations not supported yet.")
        }

        val frameType = FrameType.fromOpcode(firstByte and 15) ?: return null

        // TODO: allocate this array in some more performant, centralized way
        val data = ByteArray(payloadLength.toInt())
        if (isMasked) {
            val maskingKey = ByteArray(4, { index -> buffer.get(offset + dataStart + index) })
            dataStart += 4

            buffer.position(offset + dataStart)
            buffer.get(data)
            var x = 0
            while(x < payloadLength){
                data[x] = (data[x] xor maskingKey[x % 4])
                x++
            }
        } else {
            buffer.position(offset + dataStart)
            buffer.get(data)
        }

        when (frameType) {
            FrameType.CONTINUATION -> throw Exception("Continuations not supported yet.")
            FrameType.TEXT -> return TextFrame(data, connection)
            FrameType.BINARY -> return BinaryFrame(data, connection)
            FrameType.CLOSE -> return CloseFrame(data, connection)
            FrameType.PING -> return PingFrame(data, connection)
            FrameType.PONG -> return PongFrame(data, connection)
            else -> return null
        }
    }
}
