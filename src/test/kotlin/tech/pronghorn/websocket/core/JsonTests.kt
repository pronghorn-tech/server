package tech.pronghorn.websocket.core

import com.http.HttpVersion
import com.http.protocol.HttpResponseCode
import com.http.protocol.HttpResponseHeader
import com.jsoniter.output.JsonStream
import org.junit.Test
import tech.pronghorn.http.HttpResponse
import tech.pronghorn.test.CDBTest
import java.nio.ByteBuffer
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class Potato(val message: String)

private val headerDelimiterBytes = ": ".toByteArray(Charsets.US_ASCII)
private val spaceByte = 32.toByte()
private val carriageByte = 13.toByte()
private val returnByte = 10.toByte()
private val httpVersion = HttpVersion.HTTP11

private var dateCache = ByteArray(0)
private var latestDate = System.currentTimeMillis() % 1000
private val gmt = ZoneId.of("GMT")

fun getDateHeaderValue(): ByteArray {
    val now = System.currentTimeMillis()
    if (latestDate == now / 1000) {
        return dateCache
    } else {
        latestDate = now / 1000
        dateCache = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(gmt)).toByteArray(Charsets.US_ASCII)
        return dateCache
    }
}

fun writeHeader(headerType: HttpResponseHeader,
                headerValue: ByteArray,
                output: ByteArray,
                offset: Int): Int {
    val typeSize = headerType.bytes.size
    val valueSize = headerValue.size
    System.arraycopy(headerType.bytes, 0, output, offset, typeSize)
    System.arraycopy(headerDelimiterBytes, 0, output, offset + typeSize, 2)
    System.arraycopy(headerValue, 0, output, offset + typeSize + 2, valueSize)
    val endOffset = offset + typeSize + 2 + valueSize
    output[endOffset] = carriageByte
    output[endOffset + 1] = returnByte
    return typeSize + 2 + valueSize + 2
}

private val TMPLENGTHREMOVEME = "25".toByteArray(Charsets.US_ASCII)

fun renderResponse(buffer: ByteBuffer,
                   response: HttpResponse): Boolean {
    val dateBytes = getDateHeaderValue()
    val size = response.getOutputSize()

    if (buffer.remaining() < size) {
        return false
    }

    val offset = buffer.position()
    val output = buffer.array()

    var z = offset

    System.arraycopy(httpVersion.bytes, 0, output, z, httpVersion.bytes.size)
    z += httpVersion.bytes.size
    output[z] = spaceByte
    z += 1

    System.arraycopy(response.code.bytes, 0, output, z, response.code.bytes.size)
    z += response.code.bytes.size
    output[z] = carriageByte
    output[z + 1] = returnByte
    z += 2

    z += writeHeader(HttpResponseHeader.ContentLength, TMPLENGTHREMOVEME, output, z)
    z += writeHeader(HttpResponseHeader.Server, response.serverBytes, output, z)
    z += writeHeader(HttpResponseHeader.Date, dateBytes, output, z)

    response.headers.forEach { header ->
        z += header.writeHeader(output, z)
    }

//    response.headers.forEach { header ->
//        z += writeHeader(header.key, header.value, output, z)
//    }

    output[z] = carriageByte
    output[z + 1] = returnByte
    z += 2

    if (response.body.isNotEmpty()) {
        System.arraycopy(response.body, 0, output, z, response.body.size)
    }

    z += response.body.size

    buffer.position(z)

    return true
}

class JsonTests : CDBTest() {
    @Test
    fun encodingSpeed() {
        repeat(16) {
            val count = 10000000
            var total = 0L
            var x = 0
            val pre = System.currentTimeMillis()

            val buffer = ByteBuffer.allocate(1024)

            val serverBytes = "Pronghorn".toByteArray(Charsets.US_ASCII)

            while (x < count) {
                val potato = Potato("Hello, World!$x")
                val json = JsonStream.serialize(potato)
                val utf = json.toByteArray(Charsets.UTF_8)
                val response = HttpResponse(
                        HttpResponseCode.OK,
                        ArrayList(),
                        //                        mapOf(
//                                HttpResponseHeader.ContentType to CommonMimeTypes.Json.bytes
//                        ),
                        utf,
                        HttpVersion.HTTP11,
                        serverBytes
                )

                renderResponse(buffer, response)
                total += buffer.position()

                x += 1
                buffer.position(0)
            }
            val post = System.currentTimeMillis()
            val took = post - pre
            val perSecond = Math.round(count / (took / 1000.0))
            println("$total Took $took ms, $perSecond per second")
        }
    }
}
