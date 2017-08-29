package tech.pronghorn.http.protocol

import tech.pronghorn.util.finder.ByteBacked
import tech.pronghorn.util.finder.ByteBackedFinder
import tech.pronghorn.util.finder.FinderGenerator

enum class HttpVersion(val versionName: String,
                       override val bytes: ByteArray): ByteBacked {
    HTTP11("HTTP/1.1", "HTTP/1.1".toByteArray(Charsets.US_ASCII)),
    HTTP10("HTTP/1.0", "HTTP/1.0".toByteArray(Charsets.US_ASCII));

    companion object : ByteBackedFinder<HttpVersion> by httpVersionFinder
}

private val httpVersionFinder = FinderGenerator.generateFinder(HttpVersion.values())
