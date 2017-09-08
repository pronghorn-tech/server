package tech.pronghorn.server.handlers

import tech.pronghorn.http.HttpExchange
import tech.pronghorn.http.HttpResponse

class StaticHttpRequestHandler(val response: HttpResponse): HttpRequestHandler() {
    override suspend fun handle(exchange: HttpExchange) {
        exchange.sendResponse(response)
    }
}
