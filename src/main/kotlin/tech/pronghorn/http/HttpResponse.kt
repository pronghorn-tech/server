package tech.pronghorn.http

import tech.pronghorn.http.protocol.HttpResponseCode
import tech.pronghorn.http.protocol.HttpResponseHeader
import tech.pronghorn.http.protocol.HttpVersion
import java.nio.ByteBuffer

abstract class HttpResponse(val code: HttpResponseCode) {
    protected open val headers: MutableMap<HttpResponseHeader, HttpResponseHeaderValue<*>> = mutableMapOf()
    protected abstract val body: ByteArray

    fun getOutputSize(): Int {
        val statusLineSize = HttpVersion.HTTP11.bytes.size + 1 + code.bytes.size + 2
        val headersSize = headers.map { (key, value) -> key.displayBytes.size + value.valueLength + 4 }.sum()

        return statusLineSize + headersSize + 2 + body.size
    }

    fun addHeader(headerType: HttpResponseHeader,
                  value: ByteArray) = headers.put(headerType, HttpResponseHeaderValue.valueOf(value))

    fun addHeader(headerType: HttpResponseHeader,
                  value: Int) = headers.put(headerType, HttpResponseHeaderValue.valueOf(value))

    fun addHeader(headerType: HttpResponseHeader,
                  value: String) = headers.put(headerType, HttpResponseHeaderValue.valueOf(value))

    fun writeHeaders(buffer: ByteBuffer) {
        headers.forEach { (key, value) ->
            value.writeHeader(key, buffer)
        }
    }

    fun writeBody(buffer: ByteBuffer) {
        if (body.size > 0) {
            buffer.put(body, 0, body.size)
        }
    }
}
