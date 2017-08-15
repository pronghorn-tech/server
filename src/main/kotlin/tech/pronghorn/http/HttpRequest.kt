package com.http

import com.http.protocol.CustomHttpRequestHeader
import com.http.protocol.HttpMethod
import com.http.protocol.HttpRequestHeader
import com.http.protocol.StandardHttpRequestHeaders
import mu.KotlinLogging
import tech.pronghorn.plugins.map.MapPlugin
import tech.pronghorn.server.HttpConnection
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.or

sealed class HttpParseResult

class HttpRequest(val bytes: ByteArray,
                  val method: HttpMethod,
                  val url: StringLocation,
                  val version: HttpVersion,
                  val headers: Map<HttpRequestHeader, StringLocation>,
                  val connection: HttpConnection): HttpParseResult()

object IncompleteRequestParseError: HttpParseResult()

object InvalidMethodParseError: HttpParseResult()

object InvalidVersionParseError: HttpParseResult()

object InvalidHeaderTypeParseError: HttpParseResult()

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

fun isEqual(arr: ByteArray, buffer: ByteBuffer, offset: Int, size: Int): Boolean {
    val prePosition = buffer.position()
    if(arr.size != size){
        return false
    }

    buffer.position(offset)
    var x = 0
    while(x < size){
        if(buffer.get() != arr[x]){
            buffer.position(prePosition)
            return false
        }
        x += 1
    }

    buffer.position(prePosition)
    return true
}

//private val hasher = HashRegistry.getHasher()

private val emptyBytes = ByteArray(0)

data class StringLocation(val bytes: ByteArray,
                          val start: Int,
                          val length: Int) {
    var hash: Int? = null

    fun toByteArray(): ByteArray = Arrays.copyOfRange(bytes, start, start + length)

    constructor(byteBuffer: ByteBuffer,
                start: Int,
                length: Int): this(emptyBytes, start, length) {

    }

    override fun toString(): String {
        return String(bytes, start, length, Charsets.US_ASCII)
    }

    override fun hashCode(): Int {
        if (hash == null) {
//            hash = hasher(bytes, start, length).hashCode()
        }
        return hash!!
    }

    override fun equals(other: Any?): Boolean {
        when (other) {
            is ByteArray -> return isEqual(bytes, other, start, length)
            is StringLocation -> {
                if (start == 0) {
                    return isEqual(other.bytes, bytes, other.start, other.length)
                } else if (other.start == 0) {
                    return isEqual(bytes, other.bytes, start, length)
                } else {
                    return false
                }
            }
            else -> return false
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
        if(methodSize >= HttpMethod.byLength.size){
            return InvalidMethodParseError
        }

        val possibleMethods = HttpMethod.byLength[methodSize]

        val method = possibleMethods?.find { possible -> isEqual(possible.bytes, buffer, start, methodSize) }

        if(method == null){
            return InvalidMethodParseError
        }

        var urlEnd = -1
        while (buffer.hasRemaining()) {
            if (buffer.get() == spaceByte) {
                urlEnd = buffer.position() - 1
                break
            }
        }

        if (urlEnd == -1) {
            return IncompleteRequestParseError
        }

        val url = StringLocation(buffer, firstSpace, urlEnd)
//        val urlArr = ByteArray(urlEnd - firstSpace - 1)
//        buffer.position(firstSpace + 1)
//        buffer.get(urlArr)
//        val stringUrl = String(urlArr, Charsets.US_ASCII)
//        val uri = URI(stringUrl)
//        println("${uri.path}")
//
//        println(Arrays.hashCode(urlArr))

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

        val version = if(versionLength == 8 && buffer.get(urlEnd + 6) == '1'.toByte() && isEqual(HttpVersion.HTTP11.bytes, buffer, urlEnd + 1, versionLength)){
            HttpVersion.HTTP11
        }
        else if(versionLength == 6 && buffer.get(urlEnd + 6) == '2'.toByte() && isEqual(HttpVersion.HTTP2.bytes, buffer, urlEnd + 1, versionLength)){
            HttpVersion.HTTP2
        }
        else if(versionLength == 8 && buffer.get(urlEnd + 6) == '1'.toByte() && isEqual(HttpVersion.HTTP11.bytes, buffer, urlEnd + 1, versionLength)){
            HttpVersion.HTTP10
        }
        else {
            return InvalidVersionParseError
        }

        val headers = MapPlugin.get<HttpRequestHeader, StringLocation>()

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
                else if(byte < 91 && byte > 64) {
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

            if(typeEnd == -1 || lineEnd == -1){
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
            val headerType = if(headerLength < StandardHttpRequestHeaders.byLength.size) {
                val possibleHeader = StandardHttpRequestHeaders.byLength[headerLength]
                possibleHeader?.find { possible -> isEqual(possible.getBytes(), buffer, lineStart, headerLength) }
                        ?: CustomHttpRequestHeader(StringLocation(buffer, lineStart, headerLength))
            }
            else {
                CustomHttpRequestHeader(StringLocation(buffer, lineStart, headerLength))
            }
            //end

            val headerValue = StringLocation(buffer, valueStart, valueEnd - valueStart)

            headers.put(headerType, headerValue)

            if(buffer.limit() >= lineEnd + 2 && buffer.get(lineEnd) == carriageByte && buffer.get(lineEnd + 1) == returnByte){
                buffer.position(buffer.position() + 2)
                headersEnd = buffer.position()
                break
            }
        }

        if(headersEnd == -1){
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
        if(methodSize >= HttpMethod.byLength.size){
            return InvalidMethodParseError
        }

        val possibleMethods = HttpMethod.byLength[z - start]
        val method = possibleMethods?.find { possible -> isEqual(bytes, possible.bytes, start, (z - start)) }

        if(method == null){
            return InvalidMethodParseError
        }

        z += 1

        var found = false
        while (z < limit) {
            if (bytes[z] == spaceByte) {
                found = true
                break
            }
            z += 1
        }

        if (!found) {
            return IncompleteRequestParseError
        }

        val urlEnd = z

        val url = StringLocation(bytes, (firstSpace + 1), urlEnd)

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

        val version = if(versionLength == 8 && bytes[urlEnd + 6] == '1'.toByte() && isEqual(bytes, HttpVersion.HTTP11.bytes, urlEnd + 1, versionLength)){
            HttpVersion.HTTP11
        }
        else if(versionLength == 6 && bytes[urlEnd + 6] == '2'.toByte() && isEqual(bytes, HttpVersion.HTTP2.bytes, urlEnd + 1, versionLength)){
            HttpVersion.HTTP2
        }
        else if(versionLength == 8 && bytes[urlEnd + 6] == '1'.toByte() && isEqual(bytes, HttpVersion.HTTP11.bytes, urlEnd + 1, versionLength)){
            HttpVersion.HTTP10
        }
        else {
            return InvalidVersionParseError
        }

        val headers = MapPlugin.get<HttpRequestHeader, StringLocation>()

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
                }
                else if (bytes[z] < 91 && bytes[z] > 64) {
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

            if(typeEnd == -1 || lineEnd == -1){
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
            val headerType = if(headerLength < StandardHttpRequestHeaders.byLength.size) {
                val possibleHeader = StandardHttpRequestHeaders.byLength[headerLength]
                possibleHeader?.find { possible -> isEqual(bytes, possible.getBytes(), lineStart, headerLength) }
                        ?: CustomHttpRequestHeader(StringLocation(bytes, lineStart, headerLength))
            }
            else {
                CustomHttpRequestHeader(StringLocation(bytes, lineStart, headerLength))
            }
            //end

            val headerValue = StringLocation(bytes, valueStart, valueEnd - valueStart)
            headers.put(headerType, headerValue)

            if(z + 2 > limit){
                return IncompleteRequestParseError
            }
            else if(bytes[z] == carriageByte && bytes[z + 1] == returnByte){
                z += 2
                headersEnd = z
                break
            }
        }

        if(headersEnd == -1){
            return IncompleteRequestParseError
        }

        buffer.position(z)
        return HttpRequest(bytes, method, url, version, headers, connection)
    }
}
