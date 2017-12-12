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
import java.nio.charset.Charset

private val noContentHeaders = mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
        StandardHttpResponseHeaders.ContentLength to HttpResponseHeaderValue.valueOf(0)
)

private val chunkedHeaderValue = HttpResponseHeaderValue.valueOf("chunked")

public object HttpResponses {
    public open class OK(final override val content: ResponseContent) : HttpResponse(HttpResponseCode.OK) {
        override val headers = if(content.size < 0){
            mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
                    StandardHttpResponseHeaders.TransferEncoding to chunkedHeaderValue
            )
        }
        else {
            mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
                    StandardHttpResponseHeaders.ContentLength to HttpResponseHeaderValue.valueOf(content.size)
            )
        }
        constructor(content: ResponseContent,
                    contentType: ContentType) : this(content) {
            addHeader(StandardHttpResponseHeaders.ContentType, contentType.bytes)
        }

        constructor(body: ByteArray,
                    contentType: ContentType) : this(ResponseContent.from(body)) {
            addHeader(StandardHttpResponseHeaders.ContentType, contentType.bytes)
        }

        constructor(body: ByteArray,
                    contentType: ContentType,
                    charset: Charset) : this(ResponseContent.from(body)) {
            setContentType(contentType, charset)
        }

        constructor(body: String,
                    contentType: ContentType) : this(body.toByteArray(Charsets.US_ASCII), contentType)

        constructor(body: String,
                    contentType: ContentType,
                    charset: Charset) : this(ResponseContent.from(body, charset)) {
            setContentType(contentType, charset)
        }
    }

    public sealed class NoContent : HttpResponse(HttpResponseCode.NoContent) {
        companion object : NoContent()

        override val content = EmptyResponseContent
        override val headers = noContentHeaders
    }

    public open class MovedPermanently(locationBytes: ByteArray) : HttpResponse(HttpResponseCode.MovedPermanently) {
        override val content = EmptyResponseContent
        override val headers = mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
                StandardHttpResponseHeaders.Location to HttpResponseHeaderValue.valueOf(locationBytes)
        )

        constructor(location: String) : this(location.toByteArray(Charsets.US_ASCII))
    }

    public open class Found(locationBytes: ByteArray) : HttpResponse(HttpResponseCode.Found) {
        override val content = EmptyResponseContent
        override val headers = mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
                StandardHttpResponseHeaders.Location to HttpResponseHeaderValue.valueOf(locationBytes)
        )

        constructor(location: String) : this(location.toByteArray(Charsets.US_ASCII))
    }

    public open class NotModified : HttpResponse(HttpResponseCode.TemporaryRedirect) {
        override val content = EmptyResponseContent
    }

    public open class TemporaryRedirect(locationBytes: ByteArray) : HttpResponse(HttpResponseCode.TemporaryRedirect) {
        override val content = EmptyResponseContent
        override val headers = mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
                StandardHttpResponseHeaders.Location to HttpResponseHeaderValue.valueOf(locationBytes)
        )

        constructor(location: String) : this(location.toByteArray(Charsets.US_ASCII))
    }

    public open class NotFound(final override val content: ResponseContent = EmptyResponseContent) : HttpResponse(HttpResponseCode.NotFound) {
        companion object : NotFound()

        override val headers = mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
                StandardHttpResponseHeaders.ContentLength to HttpResponseHeaderValue.valueOf(content.size)
        )

        constructor(body: ByteArray,
                    contentType: ContentType) : this(ResponseContent.from(body)) {
            addHeader(StandardHttpResponseHeaders.ContentType, contentType.bytes)
        }
    }

    public open class InternalServerError(final override val content: ResponseContent = EmptyResponseContent) : HttpResponse(HttpResponseCode.InternalServerError) {
        override val headers = mutableMapOf<HttpResponseHeader, HttpResponseHeaderValue<*>>(
                StandardHttpResponseHeaders.ContentLength to HttpResponseHeaderValue.valueOf(content.size)
        )

        constructor(message: String) : this(ResponseContent.from(message.toByteArray(Charsets.US_ASCII)))

        constructor(ex: Throwable) : this(ex.message ?: "Unknown") {
            ex.printStackTrace()
        }
    }
}
