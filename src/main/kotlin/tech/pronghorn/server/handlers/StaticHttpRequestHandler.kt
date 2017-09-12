package tech.pronghorn.server.handlers

import tech.pronghorn.http.HttpExchange
import tech.pronghorn.http.HttpResponse

class StaticHttpRequestHandler(val response: HttpResponse) : NonSuspendableHttpRequestHandler() {
    override fun handle(exchange: HttpExchange) = response
}
