package tech.pronghorn.http.protocol

import tech.pronghorn.util.finder.ByteBacked
import tech.pronghorn.util.finder.ByteBackedFinder
import tech.pronghorn.util.finder.FinderGenerator

enum class HttpMethod(val methodName: String,
                      override val bytes: ByteArray = methodName.toByteArray(Charsets.US_ASCII)): ByteBacked {
    CONNECT("CONNECT"),
    DELETE("DELETE"),
    GET("GET"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS"),
    PATCH("PATCH"),
    POST("POST"),
    PUT("PUT"),
    TRACE("TRACE");

    companion object : ByteBackedFinder<HttpMethod> by httpMethodFinder
}

private val httpMethodFinder = FinderGenerator.generateFinder(HttpMethod.values())
