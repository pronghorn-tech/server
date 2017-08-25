package tech.pronghorn.http

import tech.pronghorn.http.protocol.HttpResponseCode
import tech.pronghorn.http.protocol.HttpVersion
import tech.pronghorn.server.*

class HttpResponse(val code: HttpResponseCode,
                   val headers: List<HttpResponseHeaderValue<*>>,
                   val body: ByteArray,
                   val httpVersion: HttpVersion,
                   val connection: HttpServerConnection) {
    fun getOutputSize(): Int {
        val statusLineSize = httpVersion.bytes.size + 1 + code.bytes.size + 2
        val headersSize = headers.map { header -> header.length }.sum()
        val commonHeaderSize = connection.worker.commonHeaderSize

        return statusLineSize + headersSize + commonHeaderSize + 2 + body.size
    }
}
