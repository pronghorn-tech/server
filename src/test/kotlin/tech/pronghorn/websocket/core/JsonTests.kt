package tech.pronghorn.websocket.core

import com.jsoniter.output.JsonStream
import org.junit.Test
import tech.pronghorn.http.HttpResponse
import tech.pronghorn.http.protocol.HttpResponseCode
import tech.pronghorn.http.protocol.HttpVersion
import tech.pronghorn.server.DummyConnection
import tech.pronghorn.server.DummyWorker
import tech.pronghorn.test.CDBTest
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

data class Potato(val message: String)

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
            val dummyConnection = DummyConnection(TODO(), socket, key)

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
