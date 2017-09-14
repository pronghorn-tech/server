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

package tech.pronghorn.http

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.RepeatedTest
import org.mockito.Mockito.mock
import tech.pronghorn.http.protocol.parseHttpRequest
import tech.pronghorn.server.HttpServerConnection
import tech.pronghorn.test.PronghornTest
import tech.pronghorn.test.repeatCount
import java.nio.ByteBuffer

class HttpRequestParseTests : PronghornTest() {
    val validRequestLines = arrayOf(
            "GET /plaintext HTTP/1.1",
            "Host: server",
            "User-Agent: Mozilla/5.0",
            "Cookie: uid=12345678901234567890",
            "Accept: text/html",
            "Accept-Language: en-US,en",
            "Connection: keep-alive"
    )

    val mockConnection = mock(HttpServerConnection::class.java)

    fun convertRequestLinesToBytes(lines: Array<String>): ByteArray {
        return lines.joinToString("\r\n").plus("\r\n\r\n").toByteArray(Charsets.US_ASCII)
    }

    fun writeRequestLinesToBuffer(lines: Array<String>): ByteBuffer {
        val bytes = convertRequestLinesToBytes(lines)
        val buffer = ByteBuffer.allocate(bytes.size)
        buffer.put(bytes)
        buffer.flip()

        return buffer
    }

    /*
     * Tests parsing a full request in all potential partial pieces.
     * Purpose: Ensure parsing doesn't throw exceptions when parsing incomplete messages.
     */
    @RepeatedTest(repeatCount)
    fun progressiveParsing() {
        var x = 0
        var validResponses = 0
        val validRequestBytes = convertRequestLinesToBytes(validRequestLines)
        while (x <= validRequestBytes.size) {
            val buffer = ByteBuffer.allocate(x)
            buffer.put(validRequestBytes, 0, x)
            buffer.flip()
            val parsed = parseHttpRequest(buffer, mockConnection)
            if (parsed is HttpExchange) {
                assertEquals(6, parsed.requestHeaders.size)
                validResponses += 1
            }
            x += 1
        }

        assertEquals(1, validResponses)
    }

    /*
     * Tests parsing with an invalid request requestMethod
     * Purpose: Ensure the proper error type is returned in this case.
     */
    @RepeatedTest(repeatCount)
    fun parseInvalidMethodError() {
        val invalidMethodLines = validRequestLines.copyOf()
        invalidMethodLines[0] = invalidMethodLines[0].replace("GET", "WRONG")
        val buffer = writeRequestLinesToBuffer(invalidMethodLines)
        val parsed = parseHttpRequest(buffer, mockConnection)

        assertEquals(InvalidMethodParseError, parsed)
    }

    /*
     * Tests parsing with an invalid http version
     * Purpose: Ensure the proper error type is returned in this case.
     */
    @RepeatedTest(repeatCount)
    fun parseInvalidVersionError() {
        val invalidMethodLines = validRequestLines.copyOf()
        invalidMethodLines[0] = invalidMethodLines[0].replace("HTTP/1.1", "HTTP/3")
        val buffer = writeRequestLinesToBuffer(invalidMethodLines)
        val parsed = parseHttpRequest(buffer, mockConnection)

        assertEquals(InvalidVersionParseError, parsed)
    }
}
