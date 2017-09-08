package tech.pronghorn.websocket.core

import com.jsoniter.output.JsonStream
import org.junit.jupiter.api.RepeatedTest
import tech.pronghorn.http.HttpResponses
import tech.pronghorn.http.protocol.CommonContentTypes
import tech.pronghorn.test.PronghornTest
import tech.pronghorn.test.repeatCount
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

data class Simple(val message: String)

class JsonTests : PronghornTest() {
    @RepeatedTest(repeatCount)
    fun encodingSpeed() {
        val count = 10000000
        var total = 0L
        var x = 0
        val pre = System.currentTimeMillis()

        val buffer = ByteBuffer.allocate(1024)

        val socket = SocketChannel.open()
        socket.configureBlocking(false)
        val emptyBytes = ByteArray(0)

        while (x < count) {
            val potato = Simple("Hello, World!$x")
            val json = JsonStream.serialize(potato)
            val utf = json.toByteArray(Charsets.UTF_8)
            val response = HttpResponses.OK(utf, CommonContentTypes.ApplicationJson)
            response.writeToBuffer(buffer, emptyBytes)
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
