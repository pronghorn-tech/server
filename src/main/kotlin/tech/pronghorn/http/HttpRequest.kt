package tech.pronghorn.http

import mu.KotlinLogging
import tech.pronghorn.http.protocol.*
import tech.pronghorn.plugins.map.MapPlugin
import tech.pronghorn.server.HttpConnection
import java.nio.ByteBuffer
import java.util.*
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

private const val spaceByte: Byte = 0x20
private const val carriageByte: Byte = 0xD
private const val returnByte: Byte = 0xA
private const val colonByte: Byte = 0x3A
private const val tabByte: Byte = 0x9

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

private fun bufferSliceToArray(buffer: ByteBuffer,
                               start: Int,
                               length: Int): ByteArray {
    val slice = ByteArray(length)
    val prePosition = buffer.position()
    if(prePosition != start) {
        buffer.position(start)
    }
    buffer.get(slice)
    buffer.position(prePosition)
    return slice
}

data class AsciiString(val bytes: ByteArray) {
    constructor(buffer: ByteBuffer,
                start: Int,
                length: Int) : this(bufferSliceToArray(buffer, start, length))

    constructor(bytes: ByteArray,
                start: Int,
                length: Int) : this(Arrays.copyOfRange(bytes, start, start + length))

    override fun toString(): String = String(bytes, Charsets.US_ASCII)

    override fun hashCode(): Int = Arrays.hashCode(bytes)

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ByteArray -> Arrays.equals(bytes, other)
            is AsciiString -> Arrays.equals(bytes, other.bytes)
            else -> false
        }
    }
}

object HttpRequestParser {
    private val logger = KotlinLogging.logger {}

    fun parseDirect(buffer: ByteBuffer,
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

        val url = parseHttpURI(buffer)
        val urlEnd = buffer.position() - 1

        var requestLineEnd = -1
        while (buffer.hasRemaining()) {
            if (buffer.get() == carriageByte && buffer.hasRemaining() && buffer.get() == returnByte) {
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
                if (buffer.get() == carriageByte && buffer.hasRemaining() && buffer.get() == returnByte) {
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

            if (buffer.limit() >= lineEnd + 2 && buffer.get(lineEnd) == carriageByte && buffer.get(lineEnd + 1) == returnByte) {
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

    fun parse(buffer: ByteBuffer,
              connection: HttpConnection): HttpParseResult {
        val bytes = buffer.array()
        val limit = buffer.limit()
        val start = buffer.position()
        var z = start

        var firstSpace = -1
        while (z < limit) {
            if (bytes[z] == spaceByte) {
                firstSpace = z
                break
            }
            z += 1
        }

        if (firstSpace == -1) {
            return IncompleteRequestParseError
        }

        val methodSize = z - start
//        if (methodSize >= HttpMethod.byLength.size) {
//            return InvalidMethodParseError
//        }
//
//        val possibleMethods = HttpMethod.byLength[z - start]
//        val method = possibleMethods?.find { possible -> isEqual(bytes, possible.bytes, start, (z - start)) }
        val method = HttpMethod.find(buffer, start, methodSize)

        if (method == null) {
            return InvalidMethodParseError
        }

        z += 1

//        var found = false
//        while (z < limit) {
//            if (bytes[z] == spaceByte) {
//                found = true
//                break
//            }
//            z += 1
//        }
//
//        if (!found) {
//            return IncompleteRequestParseError
//        }
//
//        val urlEnd = z

//        val url = AsciiString(buffer, (firstSpace + 1), urlEnd)
        val url = parseHttpURI(buffer)
        val urlEnd = buffer.position()

        var requestLineEnd = -1
        while (z < limit) {
            if (bytes[z] == carriageByte) {
                if (z + 1 < limit && bytes[z + 1] == returnByte) {
                    z += 2
                    requestLineEnd = z
                    break
                }
            }
            z += 1
        }

        if (requestLineEnd == -1) {
            return IncompleteRequestParseError
        }

        val versionLength = requestLineEnd - urlEnd - 3

        val version = if (versionLength == 8 && bytes[urlEnd + 6] == '1'.toByte() && isEqual(bytes, HttpVersion.HTTP11.bytes, urlEnd + 1, versionLength)) {
            HttpVersion.HTTP11
        } else if (versionLength == 6 && bytes[urlEnd + 6] == '2'.toByte() && isEqual(bytes, HttpVersion.HTTP2.bytes, urlEnd + 1, versionLength)) {
            HttpVersion.HTTP2
        } else if (versionLength == 8 && bytes[urlEnd + 6] == '1'.toByte() && isEqual(bytes, HttpVersion.HTTP11.bytes, urlEnd + 1, versionLength)) {
            HttpVersion.HTTP10
        } else {
            return InvalidVersionParseError
        }

        val headers = MapPlugin.get<HttpRequestHeader, AsciiString>()

        var headersEnd = -1
        while (true) {
            val lineStart = z
            var typeEnd = -1
            var lineEnd = -1

            while (z < limit) {
                if (bytes[z] == colonByte) {
                    typeEnd = z
                    z += 1
                    break
                } else if (bytes[z] < 91 && bytes[z] > 64) {
                    // lowercase header names for lookup
                    bytes[z] = bytes[z].or(0x20)
                }
                z += 1
            }

            // trim whitespace from beginning of value
            while (z < limit && (bytes[z] == spaceByte || bytes[z] == tabByte)) {
                z += 1
            }
            val valueStart = z

            while (z < limit - 1) {
                if (bytes[z] == carriageByte && bytes[z + 1] == returnByte) {
                    lineEnd = z
                    z += 2
                    break
                }

                z += 1
            }

            if (typeEnd == -1 || lineEnd == -1) {
                return IncompleteRequestParseError
            }

            var valueEnd = lineEnd
            // trim whitespace from end of the value
            while (bytes[valueEnd - 1] == spaceByte || bytes[valueEnd - 1] == tabByte) {
                valueEnd -= 1
            }

            val headerLength = typeEnd - lineStart

            // start
//            if(headerLength >= StandardHttpRequestHeaders.byLength.size){
//                return InvalidHeaderTypeParseError
//            }
//
//            val possibleHeader = StandardHttpRequestHeaders.byLength[headerLength]
//            val headerType = possibleHeader?.find { possible -> isEqual(bytes, possible._bytes, lineStart, headerLength) }
//                    ?: return InvalidHeaderTypeParseError
            // end

            // start
            val headerType = StandardHttpRequestHeaders.find(buffer, lineStart, headerLength) ?:
                    CustomHttpRequestHeader(AsciiString(buffer, lineStart, headerLength))
//            val headerType = if (headerLength < StandardHttpRequestHeaders.byLength.size) {
//                val possibleHeader = StandardHttpRequestHeaders.byLength[headerLength]
//                possibleHeader?.find { possible -> isEqual(bytes, possible.bytes, lineStart, headerLength) }
//                        ?: CustomHttpRequestHeader(AsciiString(buffer, lineStart, headerLength))
//            } else {
//                CustomHttpRequestHeader(AsciiString(buffer, lineStart, headerLength))
//            }
            //end

            val headerValue = AsciiString(buffer, valueStart, valueEnd - valueStart)
            headers.put(headerType, headerValue)

            if (z + 2 > limit) {
                return IncompleteRequestParseError
            } else if (bytes[z] == carriageByte && bytes[z + 1] == returnByte) {
                z += 2
                headersEnd = z
                break
            }
        }

        if (headersEnd == -1) {
            return IncompleteRequestParseError
        }

        buffer.position(z)
        return HttpRequest(bytes, method, url, version, headers, connection)
    }
}
