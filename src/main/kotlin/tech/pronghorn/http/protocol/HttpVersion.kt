package com.http

enum class HttpVersion(val versionName: String,
                       val bytes: ByteArray) {
    HTTP10("HTTP/1.0", "HTTP/1.0".toByteArray(Charsets.US_ASCII)),
    HTTP11("HTTP/1.1", "HTTP/1.1".toByteArray(Charsets.US_ASCII)),
    HTTP2("HTTP/2", "HTTP/2".toByteArray(Charsets.US_ASCII))
}
