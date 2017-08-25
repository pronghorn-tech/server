package tech.pronghorn.http

import tech.pronghorn.http.protocol.HttpResponseHeader
import tech.pronghorn.server.carriageReturnNewLineShort
import tech.pronghorn.server.colonSpaceShort
import java.nio.ByteBuffer

sealed class HttpResponseHeaderValue<T>(open val header: HttpResponseHeader,
                                        open val value: T) {
    abstract val valueLength: Int

    abstract val length: Int

    abstract fun writeHeader(output: ByteBuffer): Int
}

class NumericResponseHeaderValue(override val header: HttpResponseHeader,
                                 override val value: Int) : HttpResponseHeaderValue<Int>(header, value) {
    override val valueLength = when {
        value < 10 -> 1
        value < 100 -> 2
        value < 1000 -> 3
        value < 10000 -> 4
        value < 100000 -> 5
        value < 1000000 -> 6
        value < 10000000 -> 7
        value < 100000000 -> 8
        value < 1000000000 -> 9
        else -> 10
    }

    override val length = header.displayBytes.size + valueLength + 4

    override fun writeHeader(output: ByteBuffer): Int {
        output.put(header.displayBytes)
        output.putShort(colonSpaceShort)

        if (value > 1000000000) output.put((48 + (value.rem(10000000000) / 1000000000)).toByte())
        if (value > 100000000) output.put((48 + (value.rem(1000000000) / 100000000)).toByte())
        if (value > 10000000) output.put((48 + (value.rem(100000000) / 10000000)).toByte())
        if (value > 1000000) output.put((48 + (value.rem(10000000) / 1000000)).toByte())
        if (value > 100000) output.put((48 + (value.rem(1000000) / 100000)).toByte())
        if (value > 10000) output.put((48 + (value.rem(100000) / 10000)).toByte())
        if (value > 1000) output.put((48 + (value.rem(10000) / 1000)).toByte())
        if (value > 100) output.put((48 + (value.rem(1000) / 100)).toByte())
        if (value > 10) output.put((48 + (value.rem(100) / 10)).toByte())
        output.put((48 + value.rem(10)).toByte())

        output.putShort(carriageReturnNewLineShort)

        return length
    }
}

class ByteArrayResponseHeaderValue(override val header: HttpResponseHeader,
                                   override val value: ByteArray) : HttpResponseHeaderValue<ByteArray>(header, value) {
    override val valueLength: Int = value.size

    override val length = header.displayBytes.size + valueLength + 4

    override fun writeHeader(output: ByteBuffer): Int {
        output.put(header.displayBytes)
        output.putShort(colonSpaceShort)
        output.put(value)
        output.putShort(carriageReturnNewLineShort)

        return length
    }
}
