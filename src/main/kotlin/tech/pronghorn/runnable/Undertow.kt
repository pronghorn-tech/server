package tech.pronghorn.runnable

/*
import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.PathHandler
import io.undertow.server.handlers.SetHeaderHandler
import io.undertow.util.Headers.CONTENT_TYPE
import java.nio.ByteBuffer
import java.util.*

internal class PlaintextHandler : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) {
        exchange.responseHeaders.put(CONTENT_TYPE, "text/plain")
        exchange.responseSender.send(buffer.duplicate())
    }

    companion object {
        private val buffer: ByteBuffer

        init {
            val message = "Hello, World!"
            val messageBytes = message.toByteArray(Charsets.US_ASCII)
            buffer = ByteBuffer.allocateDirect(messageBytes.size)
            buffer.put(messageBytes)
            buffer.flip()
        }
    }
}

fun main(args: Array<String>) {
    val props = Properties()
    val port = 2648
    val host = "10.0.1.2"
    val paths = PathHandler()
            .addExactPath("/plaintext", PlaintextHandler())
    val rootHandler = SetHeaderHandler(paths, "Server", "U-tow")
    Undertow.builder()
            .setWorkerThreads(80)
            .setIoThreads(8)
            .addHttpListener(port, host)
            .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)
            .setHandler(rootHandler)
            .build()
            .start()
}
*/
