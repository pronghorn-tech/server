package tech.pronghorn.http

import tech.pronghorn.http.HttpRequest
import tech.pronghorn.http.HttpRequestParser
import tech.pronghorn.http.InvalidMethodParseError
import tech.pronghorn.http.InvalidVersionParseError
import tech.pronghorn.http.protocol.HttpMethod
import org.junit.Test
import tech.pronghorn.test.CDBTest
import java.nio.ByteBuffer
import kotlin.test.assertEquals

class WebsocketServerTests : CDBTest() {
    val validRequestLines1 = arrayOf(
            "GET /plaintext HTTP/1.1",
            "Host: server",
            "User-Agent: Mozilla/5.0",
            "Cookie: uid=12345678901234567890",
            "Accept: text/html",
            "Accept-Language: en-US,en",
            "Connection: keep-alive"
    )

    val validRequestLines = arrayOf(
            "GET /plaintext HTTP/1.1",
            "Host: server",
            "User-Agent: Mozilla/5.0 (X11; Linux x86_64) Gecko/20130501 Firefox/30.0 AppleWebKit/600.00 Chrome/30.0.0000.0 Trident/10.0 Safari/600.00",
            "Cookie: uid=12345678901234567890; __utma=1.1234567890.1234567890.1234567890.1234567890.12; wd=2560x1600",
            "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language: en-US,en;q=0.5",
            "Connection: keep-alive"
    )

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

    @Test
    fun benchmarkParse() {
        repeat(16) {
            val buffer = writeRequestLinesToBuffer(validRequestLines)
            var x = 0
            val count = 1000000
            val pre = System.currentTimeMillis()

            while (x < count) {
                val parsed = HttpRequestParser.parse(buffer, TODO())
                if (parsed is HttpRequest && parsed.method == HttpMethod.GET) {
                    x += 1
                }
                buffer.position(0)
            }

            val post = System.currentTimeMillis()
            val took = post - pre
            val perSecond = Math.round(count / (took / 1000.0))
            println("Took $took ms, $perSecond per second")
        }
    }

    /*
     * Tests parsing a full request in all potential partial pieces.
     * Purpose: Ensure parsing doesn't throw exceptions when parsing incomplete messages.
     */
    @Test
    fun progressiveParsing() {
        var x = 0
        var validResponses = 0
        val validRequestBytes = convertRequestLinesToBytes(validRequestLines)
        while (x <= validRequestBytes.size) {
            val buffer = ByteBuffer.allocate(x)
            buffer.put(validRequestBytes, 0, x)
            buffer.flip()
            val parsed = HttpRequestParser.parse(buffer, TODO())
            if (parsed is HttpRequest) {
                assertEquals(6, parsed.headers.size)
                validResponses += 1
            }
            x += 1
        }

        assertEquals(1, validResponses)
    }

    /*
     * Tests parsing with an invalid request method
     * Purpose: Ensure the proper error type is returned in this case.
     */
    @Test
    fun parseInvalidMethodError() {
        val invalidMethodLines = validRequestLines.copyOf()
        invalidMethodLines[0] = invalidMethodLines[0].replace("GET", "WRONG")
        val buffer = writeRequestLinesToBuffer(invalidMethodLines)
        val parsed = HttpRequestParser.parse(buffer, TODO())

        assertEquals(InvalidMethodParseError, parsed)
    }

    /*
     * Tests parsing with an invalid http version
     * Purpose: Ensure the proper error type is returned in this case.
     */
    @Test
    fun parseInvalidVersionError() {
        val invalidMethodLines = validRequestLines.copyOf()
        invalidMethodLines[0] = invalidMethodLines[0].replace("HTTP/1.1", "HTTP/3")
        val buffer = writeRequestLinesToBuffer(invalidMethodLines)
        val parsed = HttpRequestParser.parse(buffer, TODO())

        assertEquals(InvalidVersionParseError, parsed)
    }
}
