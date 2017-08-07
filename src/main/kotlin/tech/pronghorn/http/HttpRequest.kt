package com.http

import com.http.protocol.CustomHttpRequestHeader
import com.http.protocol.HttpMethod
import com.http.protocol.HttpRequestHeader
import com.http.protocol.StandardHttpRequestHeaders
import it.unimi.dsi.fastutil.objects.*
import mu.KotlinLogging
import org.jctools.maps.NonBlockingHashMap
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.or

sealed class HttpParseResult

class HttpRequest(val bytes: ByteArray,
                  val method: HttpMethod,
                  val url: StringLocation,
                  val version: HttpVersion,
                  val headers: Map<HttpRequestHeader, StringLocation>): HttpParseResult()

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

private val hasher = HashRegistry.getHasher()

data class StringLocation(val bytes: ByteArray,
                          val start: Int,
                          val length: Int) {
    var hash: Int? = null

    fun toByteArray(): ByteArray = Arrays.copyOfRange(bytes, start, start + length)
//        val value = ByteArray(length)
//
//        System.arraycopy(bytes, start, value, 0, length)
//        return value
//}

    override fun toString(): String {
        return String(bytes, start, length, Charsets.US_ASCII)
    }

    override fun hashCode(): Int {
        if (hash == null) {
            hash = hasher(bytes, start, length).hashCode()
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

object MapAllocator {
    fun <K, V> getMap(): MutableMap<K, V> = Object2ObjectArrayMap()
}

object HttpRequestParser {
    private val logger = KotlinLogging.logger {}

    fun parse(buffer: ByteBuffer): HttpParseResult {
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
        val optMethod = possibleMethods?.find { possible -> isEqual(bytes, possible.bytes, start, (z - start)) }

        if(optMethod == null){
            return InvalidMethodParseError
        }
        val tmpMethod = optMethod!!

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

        val fastHeaders = MapAllocator.getMap<HttpRequestHeader, StringLocation>()
//        val fastHeaders = Object2ObjectArrayMap<HttpRequestHeader, StringLocation>()
//        val fastHeaders = mutableMapOf<HttpRequestHeader, StringLocation>()

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
            fastHeaders.put(headerType, headerValue)

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
        return HttpRequest(bytes, tmpMethod, url, version, fastHeaders)
    }
}
