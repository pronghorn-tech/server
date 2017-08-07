package tech.pronghorn.websocket.protocol

import java.nio.ByteBuffer
import kotlin.experimental.or
import kotlin.experimental.xor

object FrameWriter {
    private val noMask = ByteArray(0)

    fun calculateTotalLength(frame: WebsocketFrame,
                             isMasked: Boolean): Int {
        return calculateTotalLength(frame.payload, isMasked)
    }

    fun calculateTotalLength(payload: ByteArray,
                             isMasked: Boolean): Int {
        val payloadLength = payload.size

        return 2 + // basic header (fin bit / opcode / mask bit, basic length
                (if (isMasked) 4 else 0) + // masking bytes
                (if (payloadLength > 125) 2 else 0) + // short payload length
                (if (payloadLength > 65535) 6 else 0) + // long payload length
                payloadLength // the actual payload
    }

    fun encodeMaskedFrame(frame: WebsocketFrame,
                          maskBytes: ByteArray,
                          buffer: ByteBuffer,
                          offset: Int): Unit {
        if (maskBytes.size != 4) {
            throw IllegalArgumentException("Invalid mask, expected exactly four bytes.")
        }
        encodeFrameInternal(frame, maskBytes, buffer, offset)
    }

    fun encodeFrame(frame: WebsocketFrame,
                    buffer: ByteBuffer,
                    offset: Int): Unit {
        encodeFrameInternal(frame, noMask, buffer, offset)
    }

    private fun encodeFrameInternal(frame: WebsocketFrame,
                                    maskBytes: ByteArray,
                                    buffer: ByteBuffer,
                                    offset: Int): Unit {
        val sendMasked = maskBytes.size == 4
        val payload = frame.payload
        val payloadLength = payload.size

        val fin = true

        var payloadStart = 0

        if (payloadLength < 126) {
            // Write the mask bit and the payload length
            buffer.put(offset + 1, ((if (sendMasked) 0x80 else 0x0) or payloadLength).toByte())
            payloadStart = 2
        } else if (payloadLength < 65536) {
            // Write the payloadLength as an Int rather than short due to lack of unsigned
            buffer.putInt(offset, payloadLength)
            // Write the mask bit and 126 in the payload length byte, this overwrites an empty part of the in above
            buffer.put(offset + 1, (if (sendMasked) 0xFE else 0x7E).toByte())
            payloadStart = 4
        } else {
            // Write the payloadLength as a long in the extended length area
            buffer.putLong(offset + 4, payloadLength.toLong())
            // Write the mask bit and 127 in the payload length byte
            buffer.put(offset + 1, (if (sendMasked) 0xFF else 0x7F).toByte())
            payloadStart = 10
        }

        // Write the fin and opcode byte at the beginning, this may overwrite empty parts of a length, see above
        buffer.put(offset, ((if (fin) 0x80 else 0x0).toByte() or frame.frameType.value))

        if (sendMasked) {
            // Write the mask
            buffer.position(offset + payloadStart)
            buffer.put(maskBytes)
            payloadStart += 4
            // Write the payload, masking each byte
            var x = 0
            while(x < payload.size){
                buffer.put(offset + payloadStart + x, payload[x] xor maskBytes[x % 4])
                x += 1
            }
            buffer.position(offset + payloadStart + x)
        }
        else {
            buffer.position(offset + payloadStart)
            buffer.put(payload)
        }
    }
}
