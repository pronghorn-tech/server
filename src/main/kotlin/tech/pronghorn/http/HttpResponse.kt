/*
 * Copyright 2017 Pronghorn Technology LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.pronghorn.http

import tech.pronghorn.http.protocol.*
import tech.pronghorn.plugins.concurrentMap.ConcurrentMapPlugin
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.Arrays

public fun getHeadersOutputSize(headers: Map<HttpResponseHeader, HttpResponseHeaderValue<*>>): Int {
    if (headers.isEmpty()) {
        return 0
    }
    else {
        return headers.map { (key, value) -> key.displayBytes.size + value.valueLength + 4 }.sum()
    }
}

private class ContentTypeCharsetKey(public val contentType: ContentType,
                                    public val charset: Charset)

public abstract class HttpResponse(public val code: HttpResponseCode) {
    companion object {
        private val contentTypeMap = ConcurrentMapPlugin.get<ContentTypeCharsetKey, ByteArray>()
    }

    public abstract val content: ResponseContent
    protected open val headers: MutableMap<HttpResponseHeader, HttpResponseHeaderValue<*>> = mutableMapOf()
    private var cachedHeaderSize = 0

    fun getStatusAndHeaderSize(commonHeaderSize: Int): Int {
        if (cachedHeaderSize == 0) {
            val statusLineSize = SupportedHttpVersions.HTTP11.bytes.size + code.bytes.size + 3
            val headersSize = getHeadersOutputSize(headers)
            cachedHeaderSize = statusLineSize + headersSize + commonHeaderSize + 2
        }
        return cachedHeaderSize
    }

    public fun setContentType(contentType: ContentType) {
        addHeader(StandardHttpResponseHeaders.ContentType, contentType.asHeaderValue())
    }

    public fun setContentEncoding(contentEncoding: ContentEncoding) {
        addHeader(StandardHttpResponseHeaders.ContentEncoding, contentEncoding.asHeaderValue())
    }

    public fun setEtag(etag: ByteArray) {
        addHeader(StandardHttpResponseHeaders.ETag, etag)
    }

    public fun setContentType(contentType: ContentType,
                              charset: Charset) {
        val contentTypeHeaderValue = HttpResponse.contentTypeMap.getOrPut(ContentTypeCharsetKey(contentType, charset), {
            val charsetBytes = charset.displayName().toByteArray(Charsets.US_ASCII)
            val charsetSpecifierBytes = "; charset=".toByteArray(Charsets.US_ASCII)
            val contentTypeWithCharsetBytes = Arrays.copyOf(contentType.bytes, contentType.bytes.size + charsetSpecifierBytes.size + charsetBytes.size)
            System.arraycopy(charsetSpecifierBytes, 0, contentTypeWithCharsetBytes, contentType.bytes.size, charsetSpecifierBytes.size)
            System.arraycopy(charsetBytes, 0, contentTypeWithCharsetBytes, contentType.bytes.size + charsetSpecifierBytes.size, charsetBytes.size)
            contentTypeWithCharsetBytes
        })

        addHeader(StandardHttpResponseHeaders.ContentType, contentTypeHeaderValue)
    }

    public fun addHeader(headerType: HttpResponseHeader,
                         value: HttpResponseHeaderValue<*>) = headers.put(headerType, value)

    public fun addHeader(headerType: HttpResponseHeader,
                         value: ByteArray) = addHeader(headerType, HttpResponseHeaderValue.valueOf(value))

    public fun addHeader(headerType: HttpResponseHeader,
                         value: Int) = addHeader(headerType, HttpResponseHeaderValue.valueOf(value))

    public fun addHeader(headerType: HttpResponseHeader,
                         value: String) = addHeader(headerType, HttpResponseHeaderValue.valueOf(value))

    public fun writeHeadersToBuffer(buffer: ByteBuffer,
                                    commonHeaders: ByteArray) {
        buffer.put(SupportedHttpVersions.HTTP11.bytes)
        buffer.put(spaceByte)
        buffer.put(code.bytes)
        buffer.putShort(carriageReturnNewLineShort)
        buffer.put(commonHeaders)
        headers.forEach { (key, value) ->
            value.writeHeader(key, buffer)
        }
        buffer.putShort(carriageReturnNewLineShort)
    }
}
