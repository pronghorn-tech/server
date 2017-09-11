package tech.pronghorn.runnable

import tech.pronghorn.http.HttpResponses
import tech.pronghorn.http.protocol.CommonContentTypes
import tech.pronghorn.server.HttpServer
import tech.pronghorn.server.handlers.StaticHttpRequestHandler
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val helloWorldResponse = HttpResponses.OK("Hello, World!".toByteArray(Charsets.US_ASCII), CommonContentTypes.TextPlain)
    val helloWorldHandler = StaticHttpRequestHandler(helloWorldResponse)

    val host = "10.0.1.2"
    val port = 2648
    val address = InetSocketAddress(host, port)
    val server = HttpServer(address)
    server.start()
    server.registerUrlHandler("/plaintext", helloWorldHandler)
}
