package tech.pronghorn.http

import mu.KotlinLogging
import tech.pronghorn.http.protocol.*
import tech.pronghorn.plugins.map.MapPlugin
import tech.pronghorn.server.*
import java.nio.ByteBuffer
import kotlin.experimental.or

object HttpRequestParser {
    private val logger = KotlinLogging.logger {}

    fun parse(buffer: ByteBuffer,
              connection: HttpServerConnection): HttpParseResult {
        val start = buffer.position()
        val bytes = ByteArray(0)

        var firstSpace = -1
        while (buffer.hasRemaining()) {
            if (buffer.get() == spaceByte) {
                firstSpace = buffer.position() - 1
                break
            }
        }

        if (firstSpace == -1) {
            return IncompleteRequestParseError
        }

        val methodSize = firstSpace - start

        val method = HttpMethod.find(buffer, start, methodSize)

        if (method == null) {
            return InvalidMethodParseError
        }

        val url = parseHttpURI(buffer)
        val urlEnd = buffer.position() - 1

        var requestLineEnd = -1
        while (buffer.hasRemaining()) {
            if (buffer.get() == carriageReturnByte && buffer.hasRemaining() && buffer.get() == newLineByte) {
                requestLineEnd = buffer.position() - 1
                break
            }
        }

        if (requestLineEnd == -1) {
            return IncompleteRequestParseError
        }

        val versionLength = requestLineEnd - urlEnd - 2

        val version = HttpVersion.find(buffer, urlEnd + 1, versionLength)
        if (version == null) {
            return InvalidVersionParseError
        }

        val headers = MapPlugin.get<HttpRequestHeader, AsciiString>()

        var headersEnd = -1

        while (true) {
            val lineStart = buffer.position()
            var typeEnd = -1
            var lineEnd = -1

            while (buffer.hasRemaining()) {
                val bytePos = buffer.position()
                val byte = buffer.get()

                if (byte == colonByte) {
                    typeEnd = buffer.position() - 1
                    break
                }
                else if (byte < 91 && byte > 64) {
                    // lowercase header names for lookup
                    buffer.put(bytePos, byte.or(0x20))
                }
            }

            // trim whitespace from beginning of value
            var maybeWhite = buffer.get()
            while (buffer.hasRemaining() && (maybeWhite == spaceByte || maybeWhite == tabByte)) {
                maybeWhite = buffer.get()
            }

            buffer.position(buffer.position() - 1)
            val valueStart = buffer.position()

            while (buffer.hasRemaining()) {
                if (buffer.get() == carriageReturnByte && buffer.hasRemaining() && buffer.get() == newLineByte) {
                    lineEnd = buffer.position()
                    break
                }
            }

            if (typeEnd == -1 || lineEnd == -1) {
                return IncompleteRequestParseError
            }

            var valueEnd = lineEnd - 2
            // trim whitespace from end of the value
            maybeWhite = buffer.get(valueEnd - 1)
            while (maybeWhite == spaceByte || maybeWhite == tabByte) {
                valueEnd -= 1
                maybeWhite = buffer.get(valueEnd - 1)
            }

            val headerLength = typeEnd - lineStart

            val headerType = StandardHttpRequestHeaders.find(buffer, lineStart, headerLength) ?:
                    CustomHttpRequestHeader(AsciiString(buffer, lineStart, headerLength))

            val headerValue = AsciiString(buffer, valueStart, valueEnd - valueStart)

            headers.put(headerType, headerValue)

            if (buffer.limit() >= lineEnd + 2 && buffer.get(lineEnd) == carriageReturnByte && buffer.get(lineEnd + 1) == newLineByte) {
                buffer.position(buffer.position() + 2)
                headersEnd = buffer.position()
                break
            }
        }

        if (headersEnd == -1) {
            return IncompleteRequestParseError
        }

//        val pre = buffer.position()
//        val fullArr = ByteArray(headersEnd)
//        buffer.position(0)
//        buffer.get(fullArr)
//        println(String(fullArr, Charsets.US_ASCII))
//        buffer.position(pre)

        return HttpRequest(bytes, method, url, version, headers, connection)
    }
}
