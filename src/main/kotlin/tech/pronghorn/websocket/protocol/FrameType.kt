package tech.pronghorn.websocket.protocol

enum class FrameType private constructor(val value: Byte) {
    CONTINUATION(0x0.toByte()),
    TEXT(0x1.toByte()),
    BINARY(0x2.toByte()),
    CLOSE(0x8.toByte()),
    PING(0x9.toByte()),
    PONG(0xA.toByte());

    companion object {
        fun fromOpcode(opcode: Byte): FrameType? {
            when (opcode) {
                CONTINUATION.value -> return CONTINUATION
                TEXT.value -> return TEXT
                BINARY.value -> return BINARY
                CLOSE.value -> return CLOSE
                PING.value -> return PING
                PONG.value -> return PONG
                else -> return null
            }
        }
    }
}
