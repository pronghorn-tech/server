package tech.pronghorn.http

import tech.pronghorn.http.protocol.*
import tech.pronghorn.server.HttpServerConnection

sealed class HttpParseResult

object IncompleteRequestParseError : HttpParseResult()

object InvalidUrlParseError : HttpParseResult()

object InvalidMethodParseError : HttpParseResult()

object InvalidVersionParseError : HttpParseResult()

class HttpExchange(val requestMethod: HttpMethod,
                   val requestUrl: HttpUrl,
                   val version: HttpVersion,
                   val requestHeaders: Map<HttpRequestHeader, AsciiString>,
                   val connection: HttpServerConnection,
                   val requestBody: ByteArray?) : HttpParseResult() {

    suspend fun sendResponse(response: HttpResponse) {
        connection.enqueueResponse(response)
    }
}



