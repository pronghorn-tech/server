package tech.pronghorn.server.core

import tech.pronghorn.http.HttpExchange
import tech.pronghorn.http.HttpResponse

abstract class HttpRequestHandler {
    internal abstract suspend fun handle(exchange: HttpExchange)
}

abstract class DirectHttpRequestHandler : HttpRequestHandler() {
    override suspend fun handle(exchange: HttpExchange) {
        val response = handleDirect(exchange)
        exchange.sendResponse(response)
    }

    internal abstract suspend fun handleDirect(exchange: HttpExchange): HttpResponse
}

class StaticHttpRequestHandler(val response: HttpResponse): HttpRequestHandler() {
    override suspend fun handle(exchange: HttpExchange) {
        exchange.sendResponse(response)
    }
}
