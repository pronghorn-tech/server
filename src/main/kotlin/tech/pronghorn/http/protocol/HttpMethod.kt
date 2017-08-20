package tech.pronghorn.http.protocol

import tech.pronghorn.util.finder.ByteBacked
import tech.pronghorn.util.finder.ByteBackedFinder
import tech.pronghorn.util.finder.FinderGenerator

enum class HttpMethod(override val bytes: ByteArray): ByteBacked {
    CONNECT("CONNECT".toByteArray(Charsets.US_ASCII)),
    DELETE("DELETE".toByteArray(Charsets.US_ASCII)),
    GET("GET".toByteArray(Charsets.US_ASCII)),
    HEAD("HEAD".toByteArray(Charsets.US_ASCII)),
    OPTIONS("OPTIONS".toByteArray(Charsets.US_ASCII)),
    PATCH("PATCH".toByteArray(Charsets.US_ASCII)),
    POST("POST".toByteArray(Charsets.US_ASCII)),
    PUT("PUT".toByteArray(Charsets.US_ASCII)),
    TRACE("TRACE".toByteArray(Charsets.US_ASCII));

    companion object : ByteBackedFinder<HttpMethod> by httpMethodFinder
}

private val httpMethodFinder = FinderGenerator.generateFinder(HttpMethod.values())
