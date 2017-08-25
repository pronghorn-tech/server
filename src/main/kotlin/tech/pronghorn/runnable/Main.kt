package tech.pronghorn.runnable

import tech.pronghorn.http.HttpRequest
import tech.pronghorn.http.HttpResponse
import tech.pronghorn.http.HttpResponseHeaderValue
import tech.pronghorn.http.protocol.HttpResponseCode
import tech.pronghorn.http.protocol.HttpVersion
import tech.pronghorn.server.HttpServer
import tech.pronghorn.server.config.HttpServerConfig
import tech.pronghorn.server.core.HttpRequestHandler
import java.net.InetSocketAddress

private class PlaintextHandler : HttpRequestHandler() {
    val contentBytes = "Hello World".toByteArray(Charsets.US_ASCII)
    val headers = listOf<HttpResponseHeaderValue<*>>()

    override suspend fun handleGet(request: HttpRequest): HttpResponse {
        return HttpResponse(HttpResponseCode.OK, headers, contentBytes, HttpVersion.HTTP11, request.connection)
    }
}

fun main(args: Array<String>) {
    val server = HttpServer(HttpServerConfig(InetSocketAddress("10.0.2.1", 2648)))
    server.registerUrlHandler("/plaintext", PlaintextHandler())
}
