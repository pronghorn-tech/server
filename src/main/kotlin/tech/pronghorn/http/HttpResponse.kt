package tech.pronghorn.http

import tech.pronghorn.http.protocol.HttpResponseCode
import tech.pronghorn.http.protocol.HttpResponseHeader
import tech.pronghorn.http.protocol.SupportedHttpVersions
import tech.pronghorn.http.protocol.carriageReturnNewLineShort
import tech.pronghorn.http.protocol.spaceByte
import java.nio.ByteBuffer

abstract class HttpResponse(val code: HttpResponseCode) {
    protected open val headers: MutableMap<HttpResponseHeader, HttpResponseHeaderValue<*>> = mutableMapOf()
    protected abstract val body: ByteArray
    private var calculatedOutputSize = 0

    fun getOutputSize(commonHeaderSize: Int): Int {
        if (calculatedOutputSize == 0) {
            val statusLineSize = SupportedHttpVersions.HTTP11.bytes.size + code.bytes.size + 3
            val headersSize = headers.map { (key, value) -> key.displayBytes.size + value.valueLength + 4 }.sum()

            calculatedOutputSize = statusLineSize + headersSize + 2 + body.size
        }

        return calculatedOutputSize + commonHeaderSize
    }

    fun addHeader(headerType: HttpResponseHeader,
                  value: ByteArray) {
        calculatedOutputSize = 0
        headers.put(headerType, HttpResponseHeaderValue.valueOf(value))
    }

    fun addHeader(headerType: HttpResponseHeader,
                  value: Int) {
        calculatedOutputSize = 0
        headers.put(headerType, HttpResponseHeaderValue.valueOf(value))
    }

    fun addHeader(headerType: HttpResponseHeader,
                  value: String) {
        calculatedOutputSize = 0
        headers.put(headerType, HttpResponseHeaderValue.valueOf(value))
    }

    fun writeToBuffer(buffer: ByteBuffer,
                      commonHeaders: ByteArray) {
        buffer.put(SupportedHttpVersions.HTTP11.bytes)
        buffer.put(spaceByte)
        buffer.put(code.bytes)
        buffer.putShort(carriageReturnNewLineShort)

        buffer.put(commonHeaders)

        writeHeaders(buffer)

        buffer.putShort(carriageReturnNewLineShort)

        writeBody(buffer)
    }

    private fun writeHeaders(buffer: ByteBuffer) {
        headers.forEach { (key, value) ->
            value.writeHeader(key, buffer)
        }
    }

    private fun writeBody(buffer: ByteBuffer) {
        if (body.size > 0) {
            buffer.put(body, 0, body.size)
        }
    }
}
