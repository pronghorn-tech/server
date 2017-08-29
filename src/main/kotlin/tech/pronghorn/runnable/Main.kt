package tech.pronghorn.runnable

import tech.pronghorn.http.HttpExchange
import tech.pronghorn.http.HttpResponse
import tech.pronghorn.http.HttpResponses
import tech.pronghorn.http.protocol.CommonContentTypes
import tech.pronghorn.server.HttpServer
import tech.pronghorn.server.config.HttpServerConfig
import tech.pronghorn.server.core.DirectHttpRequestHandler
import java.net.InetSocketAddress

private class PlaintextHandler : DirectHttpRequestHandler() {
    val contentBytes = "Hello World".toByteArray(Charsets.US_ASCII)

    suspend override fun handleDirect(exchange: HttpExchange): HttpResponse {
        return HttpResponses.OK(contentBytes, CommonContentTypes.ApplicationJson)
    }

}

fun main(args: Array<String>) {
    val server = HttpServer(HttpServerConfig(InetSocketAddress("10.0.2.1", 2648)))
    server.registerUrlHandler("/plaintext", PlaintextHandler())
}
