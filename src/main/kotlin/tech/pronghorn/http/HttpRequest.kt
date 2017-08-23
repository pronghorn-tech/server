package tech.pronghorn.http

import mu.KotlinLogging
import tech.pronghorn.http.protocol.*
import tech.pronghorn.plugins.map.MapPlugin
import tech.pronghorn.server.*
import java.nio.ByteBuffer
import kotlin.experimental.or

sealed class HttpParseResult

class HttpRequest(val bytes: ByteArray,
                  val method: HttpMethod,
                  val url: HttpRequestURI,
                  val version: HttpVersion,
                  val headers: Map<HttpRequestHeader, AsciiString>,
                  val connection: HttpConnection) : HttpParseResult()

object IncompleteRequestParseError : HttpParseResult()

object InvalidMethodParseError : HttpParseResult()

object InvalidVersionParseError : HttpParseResult()

object InvalidHeaderTypeParseError : HttpParseResult()

fun isEqual(a1: ByteArray, a2: ByteArray, offset: Int, size: Int): Boolean {
    if (a2.size != size) {
        return false
    }

    var x = 0
    while (x < size) {
        if (a1[offset + x] != a2[x]) {
            return false
        }
        x += 1
    }
    return true
}

fun isEqualStartingAt(a1: ByteArray, a2: ByteArray, startingAt: Int): Boolean {
    if (a1.size != a2.size) {
        return false
    }

    var x = 0
    while (x < a1.size) {
        val index = (startingAt + x) % a1.size
        if (a1[index] != a2[index]) {
            return false
        }
        x += 1
    }
    return true
}

fun isEqualStartingAt(arr: ByteArray, buffer: ByteBuffer, offset: Int, size: Int, startingAt: Int): Boolean {
    val prePosition = buffer.position()
    if (arr.size != size) {
        return false
    }

    buffer.position(offset + startingAt)
    var x = startingAt
    while (x < size) {
        if (buffer.get() != arr[x]) {
            buffer.position(prePosition)
            return false
        }
        x += 1
    }

    x = 0
    buffer.position(offset)
    while(x < startingAt){
        if (buffer.get() != arr[x]) {
            buffer.position(prePosition)
            return false
        }
        x += 1
    }

    buffer.position(prePosition)
    return true
}

fun isEqual(arr: ByteArray, buffer: ByteBuffer, offset: Int, size: Int): Boolean {
    val prePosition = buffer.position()
    if (arr.size != size) {
        return false
    }

    buffer.position(offset)
    var x = 0
    while (x < size) {
        if (buffer.get() != arr[x]) {
            buffer.position(prePosition)
            return false
        }
        x += 1
    }

    buffer.position(prePosition)
    return true
}

object HttpRequestParser {
    private val logger = KotlinLogging.logger {}

    fun parse(buffer: ByteBuffer,
                    connection: HttpConnection): HttpParseResult {
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

//        val url = ValueHttpRequestURI("/plaintext")
//        val urlEnd = buffer.position() + 10
//        buffer.position(urlEnd)
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
        if(version == null){
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
                } else if (byte < 91 && byte > 64) {
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
