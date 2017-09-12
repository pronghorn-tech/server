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
                   val requestHeaders: Map<HttpRequestHeader, ByteArray>,
                   val connection: HttpServerConnection,
                   val requestBody: ByteArray?) : HttpParseResult()


