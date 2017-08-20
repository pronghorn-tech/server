package tech.pronghorn.websocket.core

import tech.pronghorn.http.protocol.HttpVersion
import tech.pronghorn.http.protocol.HttpResponseCode
import tech.pronghorn.http.protocol.HttpResponseHeader
import com.jsoniter.output.JsonStream
import org.junit.Test
import tech.pronghorn.http.HttpResponse
import tech.pronghorn.server.DummyConnection
import tech.pronghorn.server.DummyWorker
import tech.pronghorn.test.CDBTest
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class Potato(val message: String)

//private val headerDelimiterBytes = ": ".toByteArray(Charsets.US_ASCII)
//private val spaceByte = 32.toByte()
//private val carriageByte = 13.toByte()
//private val returnByte = 10.toByte()
//private val httpVersion = HttpVersion.HTTP11
//
//private var dateCache = ByteArray(0)
//private var latestDate = System.currentTimeMillis() % 1000
//private val gmt = ZoneId.of("GMT")
//
//fun getDateHeaderValue(): ByteArray {
//    val now = System.currentTimeMillis()
//    if (latestDate == now / 1000) {
//        return dateCache
//    } else {
//        latestDate = now / 1000
//        dateCache = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(gmt)).toByteArray(Charsets.US_ASCII)
//        return dateCache
//    }
//}
//
//fun writeHeader(headerType: HttpResponseHeader,
//                headerValue: ByteArray,
//                output: ByteArray,
//                offset: Int): Int {
//    val typeSize = headerType.bytes.size
//    val valueSize = headerValue.size
//    System.arraycopy(headerType.bytes, 0, output, offset, typeSize)
//    System.arraycopy(headerDelimiterBytes, 0, output, offset + typeSize, 2)
//    System.arraycopy(headerValue, 0, output, offset + typeSize + 2, valueSize)
//    val endOffset = offset + typeSize + 2 + valueSize
//    output[endOffset] = carriageByte
//    output[endOffset + 1] = returnByte
//    return typeSize + 2 + valueSize + 2
//}
//
//private val TMPLENGTHREMOVEME = "25".toByteArray(Charsets.US_ASCII)
//
//fun renderResponse(buffer: ByteBuffer,
//                         response: HttpResponse): Boolean {
//    val size = response.getOutputSize()
//    val start = buffer.position()
//
//    if (buffer.remaining() < size) {
//        return false
//    }
//
//    buffer.put(response.httpVersion.bytes)
//    buffer.put(tech.pronghorn.server.spaceByte)
//    buffer.put(response.code.bytes)
//    buffer.put(tech.pronghorn.server.carriageByte)
//    buffer.put(tech.pronghorn.server.returnByte)
//
//    response.headers.forEach { header ->
//        header.writeHeaderDirect(buffer, buffer.position())
//    }
//
//    buffer.put(tech.pronghorn.server.carriageByte)
//    buffer.put(tech.pronghorn.server.returnByte)
//
//    if (response.body.isNotEmpty()) {
//        buffer.put(response.body, 0, response.body.size)
//    }
//
//    return true
//}

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

            val socket = SocketChannel.open()
            socket.configureBlocking(false)
            val selector = Selector.open()
            val key = socket.register(selector, SelectionKey.OP_READ)
            val dummyConnection = DummyConnection(DummyWorker(), socket, key)

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
                        serverBytes,
                        dummyConnection
                )

                //renderResponse(buffer, response)
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
