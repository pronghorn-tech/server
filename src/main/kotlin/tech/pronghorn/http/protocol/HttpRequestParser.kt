/*
 * Copyright 2017 Pronghorn Technology LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.pronghorn.http.protocol

import tech.pronghorn.http.*
import tech.pronghorn.server.HttpServerConnection
import tech.pronghorn.util.sliceToArray
import java.nio.ByteBuffer
import kotlin.experimental.or

private const val defaultHeaderMapSize = 8
private val maxMethodLength = HttpMethod.values().map { it.methodName.length }.max() ?: 0

public fun parseHttpRequest(buffer: ByteBuffer,
                            connection: HttpServerConnection): HttpParseResult {
    val start = buffer.position()

    var firstSpace = -1
    while (buffer.hasRemaining()) {
        if (buffer.get() == spaceByte) {
            firstSpace = buffer.position() - 1
            break
        }
    }

    if (firstSpace == -1) {
        if (buffer.position() - start > maxMethodLength) {
            return InvalidMethodParseError
        }
        else {
            return IncompleteRequestParseError
        }
    }

    val methodSize = firstSpace - start

    val method = HttpMethod.find(buffer, start, methodSize)

    if (method == null) {
        return InvalidMethodParseError
    }

    val urlResult = parseHttpUrl(buffer)
    val url: HttpUrl = when (urlResult) {
        is HttpUrl -> urlResult
        InvalidHttpUrl -> return InvalidUrlParseError
        IncompleteHttpUrl -> {
            if (!buffer.hasRemaining()) {
                return IncompleteRequestParseError
            }
            else {
                return InvalidUrlParseError
            }
        }
    }

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

    val version = SupportedHttpVersions.find(buffer, urlEnd + 1, versionLength) ?:
            InstanceHttpVersion.parse(buffer, urlEnd + 1, versionLength)
    if (version == null) {
        return InvalidVersionParseError
    }

    val headers = LinkedHashMap<HttpRequestHeader, ByteArray>(defaultHeaderMapSize)

    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    var headersEnd = -1

    var contentLength = 0

    while (true) {
        val lineStart = buffer.position()
        var headerTypeEnd = -1
        var lineEnd = -1

        if (buffer.remaining() >= 2 && buffer.get(lineStart) == carriageReturnByte && buffer.get(lineStart + 1) == newLineByte) {
            buffer.position(lineStart + 2)
            headersEnd = lineStart
            break
        }

        while (buffer.hasRemaining()) {
            val bytePos = buffer.position()
            val byte = buffer.get()

            if (byte == colonByte) {
                headerTypeEnd = buffer.position() - 1
                break
            }
            else if (byte < 91 && byte > 64) {
                // lowercase header names for lookup
                buffer.put(bytePos, byte.or(0x20))
            }
        }

        if (!buffer.hasRemaining()) {
            return IncompleteRequestParseError
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

        if (headerTypeEnd == -1 || lineEnd == -1) {
            return IncompleteRequestParseError
        }

        var valueEnd = lineEnd - 2
        // trim whitespace from end of the value
        maybeWhite = buffer.get(valueEnd - 1)
        while (maybeWhite == spaceByte || maybeWhite == tabByte) {
            valueEnd -= 1
            maybeWhite = buffer.get(valueEnd - 1)
        }

        val headerLength = headerTypeEnd - lineStart

        val headerType = StandardHttpRequestHeaders.find(buffer, lineStart, headerLength) ?:
                InstanceHttpRequestHeader(buffer.sliceToArray(lineStart, headerLength))

        val headerValue = buffer.sliceToArray(valueStart, valueEnd - valueStart)

        if (headerType == StandardHttpRequestHeaders.ContentLength) {
            var v = 0
            while (v < headerValue.size) {
                contentLength *= 10
                contentLength += headerValue[v]
                v += 1
            }
        }

        headers.put(headerType, headerValue)
    }

    var body: ByteArray? = null

    if (contentLength > 0) {
        if (buffer.remaining() < contentLength) {
            return IncompleteRequestParseError
        }
        else {
            body = ByteArray(contentLength)
            buffer.get(body)
        }
    }

    if (headersEnd == -1) {
        return IncompleteRequestParseError
    }

    return HttpRequest(method, url, version, headers, connection, body)
}
