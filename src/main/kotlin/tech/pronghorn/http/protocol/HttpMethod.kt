package tech.pronghorn.http.protocol

import tech.pronghorn.util.finder.*

enum class HttpMethod(val methodName: String) : ByteBacked {
    CONNECT("CONNECT"),
    DELETE("DELETE"),
    GET("GET"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS"),
    PATCH("PATCH"),
    POST("POST"),
    PUT("PUT"),
    TRACE("TRACE");

    override val bytes: ByteArray = methodName.toByteArray(Charsets.US_ASCII)

    companion object : ByteBackedFinder<HttpMethod> by httpMethodFinder
}

private val httpMethodFinder = FinderGenerator.generateFinder(HttpMethod.values())
