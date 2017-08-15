package com.http

/*
import tech.pronghorn.http.HttpResponse
import net.openhft.hashing.LongHashFunction

abstract class URLHandler {
    abstract fun handle(request: HttpRequest): HttpResponse
}

object HashRegistry {
    private val hasher = LongHashFunction.farmNa()

    fun getHasher(): (ByteArray, Int, Int) -> Long {
        return hasher::hashBytes
    }
}

class HttpServer {
    val handlers = mutableMapOf<Long, URLHandler>()
    private val hashFunction = HashRegistry.getHasher()

    fun registerUrl(url: String,
                    handler: URLHandler) {
        val urlBytes = url.toByteArray(Charsets.US_ASCII)
        val urlHash = hashFunction(urlBytes, 0, urlBytes.size)
        handlers.put(urlHash, handler)
    }

    fun handleRequest(request: HttpRequest): HttpResponse {
        val urlHash = hashFunction(request.url.bytes, request.url.start, request.url.length)
        val handler = handlers.get(urlHash)!!
        return handler.handle(request)
    }
}
*/
