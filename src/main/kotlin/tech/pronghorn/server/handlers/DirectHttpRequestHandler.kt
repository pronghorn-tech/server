package tech.pronghorn.server.handlers

import tech.pronghorn.http.HttpExchange
import tech.pronghorn.http.HttpResponse

abstract class DirectHttpRequestHandler : NonSuspendableHttpRequestHandler() {
    override fun handle(exchange: HttpExchange): HttpResponse {
        return handleDirect(exchange)
    }

    internal abstract fun handleDirect(exchange: HttpExchange): HttpResponse
}
