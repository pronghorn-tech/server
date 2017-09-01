package tech.pronghorn.runnable

import com.jsoniter.output.JsonStream
import tech.pronghorn.http.HttpExchange
import tech.pronghorn.http.HttpResponse
import tech.pronghorn.http.HttpResponses
import tech.pronghorn.http.protocol.CommonContentTypes
import tech.pronghorn.server.HttpServer
import tech.pronghorn.server.config.HttpServerConfig
import tech.pronghorn.server.core.DirectHttpRequestHandler
import tech.pronghorn.server.core.StaticHttpRequestHandler
import java.lang.management.ManagementFactory
import java.net.InetSocketAddress

data class JsonExample(val message: String)

class JsonHandler : DirectHttpRequestHandler() {
    suspend override fun handleDirect(exchange: HttpExchange): HttpResponse {
        val example = JsonExample("Hello, World!")
        val json = JsonStream.serialize(example)
        val jsonBytes = json.toByteArray(Charsets.UTF_8)
        return HttpResponses.OK(jsonBytes, CommonContentTypes.ApplicationJson)
    }
}

fun main(args: Array<String>) {
    val helloWorldResponse = HttpResponses.OK("Hello, World!".toByteArray(Charsets.US_ASCII), CommonContentTypes.TextPlain)
    val helloWorldHandler = StaticHttpRequestHandler(helloWorldResponse)
    val jsonHandler = JsonHandler()

    val host = "10.0.1.2"
    val port = 2648
    val address = InetSocketAddress(host, port)
    val config = HttpServerConfig(address, Runtime.getRuntime().availableProcessors())
    val server = HttpServer(config)
    server.registerUrlHandler("/plaintext", helloWorldHandler)
    server.registerUrlHandler("/json", jsonHandler)
    server.start()
    logger.info("Process PID: ${ManagementFactory.getRuntimeMXBean().getName()}")
    Thread.sleep(3600 * 1000)
}
