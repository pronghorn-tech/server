package tech.pronghorn.server.handlers

import tech.pronghorn.http.HttpExchange
import tech.pronghorn.http.HttpResponse

sealed class HttpRequestHandler

abstract class SuspendableHttpRequestHandler: HttpRequestHandler() {
    internal abstract suspend fun handle(exchange: HttpExchange)
}

abstract class NonSuspendableHttpRequestHandler: HttpRequestHandler() {
    internal abstract fun handle(exchange: HttpExchange): HttpResponse
}
