package tech.pronghorn.http

import java.nio.ByteBuffer
import java.util.*

private fun bufferSliceToArray(buffer: ByteBuffer,
                               start: Int,
                               length: Int): ByteArray {
    val slice = ByteArray(length)
    val prePosition = buffer.position()
    if(prePosition != start) {
        buffer.position(start)
    }
    buffer.get(slice)
    buffer.position(prePosition)
    return slice
}

data class AsciiString(val bytes: ByteArray) {
    constructor(buffer: ByteBuffer,
                start: Int,
                length: Int) : this(bufferSliceToArray(buffer, start, length))

    constructor(bytes: ByteArray,
                start: Int,
                length: Int) : this(Arrays.copyOfRange(bytes, start, start + length))

    override fun toString(): String = String(bytes, Charsets.US_ASCII)

    override fun hashCode(): Int = Arrays.hashCode(bytes)

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ByteArray -> Arrays.equals(bytes, other)
            is AsciiString -> Arrays.equals(bytes, other.bytes)
            else -> false
        }
    }
}
