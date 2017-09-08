package tech.pronghorn.server.handlers

import tech.pronghorn.http.HttpExchange
import tech.pronghorn.http.HttpResponse

abstract class DirectHttpRequestHandler : HttpRequestHandler() {
    override suspend fun handle(exchange: HttpExchange) {
        val response = handleDirect(exchange)
        exchange.sendResponse(response)
    }

    internal abstract suspend fun handleDirect(exchange: HttpExchange): HttpResponse
}
