package tech.pronghorn.http

import tech.pronghorn.http.protocol.*
import tech.pronghorn.server.HttpServerConnection

sealed class HttpParseResult

object IncompleteRequestParseError : HttpParseResult()

object InvalidMethodParseError : HttpParseResult()

object InvalidVersionParseError : HttpParseResult()

class HttpExchange(val requestMethod: HttpMethod,
                   val requestUrl: HttpRequestURI,
                   val version: HttpVersion,
                   val requestHeaders: Map<HttpRequestHeader, AsciiString>,
                   val connection: HttpServerConnection,
                   val requestBody: ByteArray?) : HttpParseResult() {

    suspend fun sendResponse(response: HttpResponse) {
        connection.appendResponse(response)
    }
}



