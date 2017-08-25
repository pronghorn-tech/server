package tech.pronghorn.http

import tech.pronghorn.http.protocol.*
import tech.pronghorn.server.*

sealed class HttpParseResult

class HttpRequest(val bytes: ByteArray,
                  val method: HttpMethod,
                  val url: HttpRequestURI,
                  val version: HttpVersion,
                  val headers: Map<HttpRequestHeader, AsciiString>,
                  val connection: HttpServerConnection) : HttpParseResult()

object IncompleteRequestParseError : HttpParseResult()

object InvalidMethodParseError : HttpParseResult()

object InvalidVersionParseError : HttpParseResult()

