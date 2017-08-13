package tech.pronghorn.http

import com.http.HttpVersion
import com.http.protocol.HttpResponseCode
import com.http.protocol.HttpResponseHeader
import tech.pronghorn.server.*
import java.nio.ByteBuffer

sealed class HttpResponseHeaderValue<T>(open val header: HttpResponseHeader,
                                        open val value: T) {
    abstract val valueLength: Int

    abstract val length: Int

    abstract fun writeHeader(output: ByteArray,
                             offset: Int): Int

    abstract fun writeHeaderDirect(output: ByteBuffer,
                                   offset: Int): Int
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

    override val length = header.bytes.size + valueLength + 4

    override fun writeHeaderDirect(output: ByteBuffer,
                                   offset: Int): Int {
        val typeSize = header.bytes.size
//        val typeSizeOffset = output.position() + typeSize
        output.put(header.bytes, 0, typeSize)
        output.put(colonByte)
        output.put(spaceByte)
//        val loc = typeSizeOffset + valueLength + 1

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

//        output.put(loc, (48 + value.rem(10)).toByte())
//        if (value > 10) output.put(loc - 1, (48 + (value.rem(100) / 10)).toByte())
//        if (value > 100) output.put(loc - 2, (48 + (value.rem(1000) / 100)).toByte())
//        if (value > 1000) output.put(loc - 3, (48 + (value.rem(10000) / 1000)).toByte())
//        if (value > 10000) output.put(loc - 4, (48 + (value.rem(100000) / 10000)).toByte())
//        if (value > 100000) output.put(loc - 5, (48 + (value.rem(1000000) / 100000)).toByte())
//        if (value > 1000000) output.put(loc - 6, (48 + (value.rem(10000000) / 1000000)).toByte())
//        if (value > 10000000) output.put(loc - 7, (48 + (value.rem(100000000) / 10000000)).toByte())
//        if (value > 100000000) output.put(loc - 8, (48 + (value.rem(1000000000) / 100000000)).toByte())
//        if (value > 1000000000) output.put(loc - 9, (48 + (value.rem(10000000000) / 1000000000)).toByte())
//        val endOffset = typeSizeOffset + 2 + valueLength
//        output.put(endOffset, carriageByte)
//        output.put(endOffset + 1, returnByte)
          output.put(carriageByte)
          output.put(returnByte)

//        output.position(endOffset + 2)

        return length
    }

    override fun writeHeader(output: ByteArray,
                             offset: Int): Int {
        val typeSize = header.bytes.size
        val typeSizeOffset = offset + typeSize
        System.arraycopy(header.bytes, 0, output, offset, typeSize)
        output[typeSizeOffset] = colonByte
        output[typeSizeOffset + 1] = spaceByte
        val loc = typeSizeOffset + valueLength + 1
        output[loc] = (48 + value.rem(10)).toByte()
        if (value > 10) output[loc - 1] = (48 + (value.rem(100) / 10)).toByte()
        if (value > 100) output[loc - 2] = (48 + (value.rem(1000) / 100)).toByte()
        if (value > 1000) output[loc - 3] = (48 + (value.rem(10000) / 1000)).toByte()
        if (value > 10000) output[loc - 4] = (48 + (value.rem(100000) / 10000)).toByte()
        if (value > 100000) output[loc - 5] = (48 + (value.rem(1000000) / 100000)).toByte()
        if (value > 1000000) output[loc - 6] = (48 + (value.rem(10000000) / 1000000)).toByte()
        if (value > 10000000) output[loc - 7] = (48 + (value.rem(100000000) / 10000000)).toByte()
        if (value > 100000000) output[loc - 8] = (48 + (value.rem(1000000000) / 100000000)).toByte()
        if (value > 1000000000) output[loc - 9] = (48 + (value.rem(10000000000) / 1000000000)).toByte()
        val endOffset = typeSizeOffset + 2 + valueLength
        output[endOffset] = carriageByte
        output[endOffset + 1] = returnByte

        return length
    }
}

class ByteArrayResponseHeaderValue(override val header: HttpResponseHeader,
                                   override val value: ByteArray) : HttpResponseHeaderValue<ByteArray>(header, value) {
    override val valueLength: Int = value.size

    override val length = header.bytes.size + valueLength + 4

    override fun writeHeaderDirect(output: ByteBuffer,
                                   offset: Int): Int {
        output.put(header.bytes)
        output.put(colonByte)
        output.put(spaceByte)
        output.put(value)
        output.put(carriageByte)
        output.put(returnByte)

        return length
    }

    override fun writeHeader(output: ByteArray, offset: Int): Int {
        val typeSize = header.bytes.size
        val typeSizeOffset = offset + typeSize
        System.arraycopy(header.bytes, 0, output, offset, typeSize)
        output[typeSizeOffset] = colonByte
        output[typeSizeOffset + 1] = spaceByte
        System.arraycopy(value, 0, output, typeSizeOffset + 2, valueLength)
        val endOffset = typeSizeOffset + 2 + valueLength
        output[endOffset] = carriageByte
        output[endOffset + 1] = returnByte

        return length
    }
}

class HttpResponse(val code: HttpResponseCode,
                   val headers: ArrayList<HttpResponseHeaderValue<*>>,
                   val body: ByteArray,
                   val httpVersion: HttpVersion,
                   val serverBytes: ByteArray,
                   val connection: HttpConnection) {
    companion object {
        private const val dateHeaderSize = 4 + 2 + 29 + 2 // [Date: Wed, 25 Nov 1981 01:23:45 UTC\r\n]
        private const val serverHeaderBaseSize = 10 // [Server: ******\r\n]
//        private const val contentLengthHeaderBaseSize = 18 // [Content-Length: ******\r\n]
    }

//    init {
//        headers.add(NumericResponseHeaderValue(HttpResponseHeader.ContentLength, body.size))
//        headers.add(ByteArrayResponseHeaderValue(HttpResponseHeader.Server, serverBytes))
//    }

    fun getOutputSize(): Int {
//        val serverHeaderSize = serverHeaderBaseSize + serverBytes.size
        val statusLineSize = httpVersion.bytes.size + 1 + code.bytes.size + 2

        val headersSize = headers.map { header -> header.length }.sum()

        return statusLineSize + /*contentLengthHeaderSize + serverHeaderSize + */dateHeaderSize + headersSize + 2 + body.size
    }
}
