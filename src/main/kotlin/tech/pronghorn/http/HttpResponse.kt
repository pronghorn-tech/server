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
import java.nio.ByteBuffer

abstract class HttpResponse(val code: HttpResponseCode) {
    protected open val headers: MutableMap<HttpResponseHeader, HttpResponseHeaderValue<*>> = mutableMapOf()
    protected abstract val body: ByteArray
    private var calculatedOutputSize = 0

    fun getOutputSize(commonHeaderSize: Int): Int {
        if (calculatedOutputSize == 0) {
            val statusLineSize = SupportedHttpVersions.HTTP11.bytes.size + code.bytes.size + 3
            val headersSize = headers.map { (key, value) -> key.displayBytes.size + value.valueLength + 4 }.sum()

            calculatedOutputSize = statusLineSize + headersSize + 2 + body.size
        }

        return calculatedOutputSize + commonHeaderSize
    }

    fun addHeader(headerType: HttpResponseHeader,
                  value: ByteArray) {
        calculatedOutputSize = 0
        headers.put(headerType, HttpResponseHeaderValue.valueOf(value))
    }

    fun addHeader(headerType: HttpResponseHeader,
                  value: Int) {
        calculatedOutputSize = 0
        headers.put(headerType, HttpResponseHeaderValue.valueOf(value))
    }

    fun addHeader(headerType: HttpResponseHeader,
                  value: String) {
        calculatedOutputSize = 0
        headers.put(headerType, HttpResponseHeaderValue.valueOf(value))
    }

    fun writeToBuffer(buffer: ByteBuffer,
                      commonHeaders: ByteArray) {
        buffer.put(SupportedHttpVersions.HTTP11.bytes)
        buffer.put(spaceByte)
        buffer.put(code.bytes)
        buffer.putShort(carriageReturnNewLineShort)

        buffer.put(commonHeaders)

        writeHeaders(buffer)

        buffer.putShort(carriageReturnNewLineShort)

        writeBody(buffer)
    }

    private fun writeHeaders(buffer: ByteBuffer) {
        headers.forEach { (key, value) ->
            value.writeHeader(key, buffer)
        }
    }

    private fun writeBody(buffer: ByteBuffer) {
        if (body.isNotEmpty()) {
            buffer.put(body, 0, body.size)
        }
    }
}
