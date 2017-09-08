package tech.pronghorn.server.handlers

import tech.pronghorn.http.HttpExchange

abstract class HttpRequestHandler {
    internal abstract suspend fun handle(exchange: HttpExchange)
}

