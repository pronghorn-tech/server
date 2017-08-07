package com.http.protocol

enum class HttpMethod(val bytes: ByteArray) {
    CONNECT("CONNECT".toByteArray(Charsets.US_ASCII)),
    DELETE("DELETE".toByteArray(Charsets.US_ASCII)),
    GET("GET".toByteArray(Charsets.US_ASCII)),
    HEAD("HEAD".toByteArray(Charsets.US_ASCII)),
    OPTIONS("OPTIONS".toByteArray(Charsets.US_ASCII)),
    PATCH("PATCH".toByteArray(Charsets.US_ASCII)),
    POST("POST".toByteArray(Charsets.US_ASCII)),
    PUT("PUT".toByteArray(Charsets.US_ASCII)),
    TRACE("TRACE".toByteArray(Charsets.US_ASCII));

    companion object {
        private val maxLength = HttpMethod.values().map { method -> method.bytes.size }.max() ?: 0
        val byLength = arrayOfNulls<Array<HttpMethod>>(maxLength)
        init {
            var x = 0
            while(x < byLength.size){
                byLength[x] = HttpMethod.values().filter { method -> method.bytes.size == x }.toTypedArray()
                x += 1
            }
        }
    }
}
