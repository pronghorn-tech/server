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
import java.nio.charset.Charset
import java.util.Arrays

private val emptyBytes = ByteArray(0)
private val noContentHeaders = mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
        StandardHttpResponseHeaders.ContentLength to HttpResponseHeaderValue.valueOf(0)
)

object HttpResponses {
    private val contentTypeMap = ConcurrentMapPlugin.get<ContentTypeCharsetKey, ByteArray>()

    private data class ContentTypeCharsetKey(val contentType: ContentType,
                                             val charset: Charset)

    // 200 OK
    open class OK(override final val body: ByteArray) : HttpResponse(HttpResponseCode.OK) {
        override val headers = mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
                StandardHttpResponseHeaders.ContentLength to HttpResponseHeaderValue.valueOf(body.size)
        )

        constructor(body: ByteArray,
                    contentType: ContentType) : this(body) {
            addHeader(StandardHttpResponseHeaders.ContentType, contentType.bytes)
        }

        constructor(body: String,
                    contentType: ContentType) : this(body.toByteArray(Charsets.US_ASCII), contentType)

        constructor(body: String,
                    contentType: ContentType,
                    charset: Charset) : this(body.toByteArray(charset)) {
            val contentTypeHeaderValue = contentTypeMap.getOrPut(ContentTypeCharsetKey(contentType, charset), {
                val charsetBytes = charset.displayName().toByteArray(Charsets.US_ASCII)
                val charsetSpecifierBytes = "; charset=".toByteArray(Charsets.US_ASCII)
                val contentTypeWithCharsetBytes = Arrays.copyOf(contentType.bytes, contentType.bytes.size + charsetSpecifierBytes.size + charsetBytes.size)
                System.arraycopy(charsetSpecifierBytes, 0, contentTypeWithCharsetBytes, contentType.bytes.size, charsetSpecifierBytes.size)
                System.arraycopy(charsetBytes, 0, contentTypeWithCharsetBytes, contentType.bytes.size + charsetSpecifierBytes.size, charsetBytes.size)
                contentTypeWithCharsetBytes
            })

            addHeader(StandardHttpResponseHeaders.ContentType, contentTypeHeaderValue)
        }
    }

    // 204 NoContent
    class NoContent : HttpResponse(HttpResponseCode.NoContent) {
        override val body = emptyBytes
        override val headers = noContentHeaders
    }

    // 301 Moved Permanently
    class MovedPermanently(val locationBytes: ByteArray) : HttpResponse(HttpResponseCode.MovedPermanently) {
        override final val body: ByteArray = emptyBytes
        override val headers = mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
                StandardHttpResponseHeaders.Location to HttpResponseHeaderValue.valueOf(locationBytes)
        )

        constructor(location: String) : this(location.toByteArray(Charsets.US_ASCII))
    }

    // 302 Found
    class Found(val locationBytes: ByteArray) : HttpResponse(HttpResponseCode.Found) {
        override final val body: ByteArray = emptyBytes
        override val headers = mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
                StandardHttpResponseHeaders.Location to HttpResponseHeaderValue.valueOf(locationBytes)
        )

        constructor(location: String) : this(location.toByteArray(Charsets.US_ASCII))
    }

    // 307 Temporary Redirect
    class TemporaryRedirect(val locationBytes: ByteArray) : HttpResponse(HttpResponseCode.TemporaryRedirect) {
        override final val body: ByteArray = emptyBytes
        override val headers = mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
                StandardHttpResponseHeaders.Location to HttpResponseHeaderValue.valueOf(locationBytes)
        )

        constructor(location: String) : this(location.toByteArray(Charsets.US_ASCII))
    }

    // 404 Not Found
    open class NotFound(override final val body: ByteArray = emptyBytes) : HttpResponse(HttpResponseCode.NotFound) {
        override val headers = mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
                StandardHttpResponseHeaders.ContentLength to HttpResponseHeaderValue.valueOf(body.size)
        )

        constructor(body: ByteArray,
                    contentType: ContentType) : this(body) {
            addHeader(StandardHttpResponseHeaders.ContentType, contentType.bytes)
        }
    }

    // 500 InternalServerError
    open class InternalServerError(override final val body: ByteArray = emptyBytes) : HttpResponse(HttpResponseCode.InternalServerError) {
        override val headers = mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
                StandardHttpResponseHeaders.ContentLength to HttpResponseHeaderValue.valueOf(body.size)
        )

        constructor(message: String) : this(message.toByteArray(Charsets.US_ASCII))

        constructor(ex: Throwable) : this(ex.message ?: "Unknown")
    }
}
