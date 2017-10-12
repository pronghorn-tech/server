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

sealed class HttpResponseHeaderValue<T>(open val value: T) {
    companion object {
        fun valueOf(value: Int): HttpResponseHeaderValue<Int> = NumericResponseHeaderValue(value)

        fun valueOf(value: String): HttpResponseHeaderValue<ByteArray> = ByteArrayResponseHeaderValue(value.toByteArray(Charsets.US_ASCII))

        fun valueOf(value: ByteArray): HttpResponseHeaderValue<ByteArray> = ByteArrayResponseHeaderValue(value)
    }

    abstract val valueLength: Int

    abstract fun writeHeader(headerType: HttpResponseHeader,
                             output: ByteBuffer)
}

class NumericResponseHeaderValue(override val value: Int) : HttpResponseHeaderValue<Int>(value) {
    override val valueLength = when {
        value < 10 -> 1
        value < 100 -> 2
        value < 1000 -> 3
        value < 10000 -> 4
        value < 100000 -> 5
        value < 1000000 -> 6
        value < 10000000 -> 7
        value < 100000000 -> 8
        value < 1000000000 -> 9
        else -> 10
    }

    override fun writeHeader(headerType: HttpResponseHeader,
                             output: ByteBuffer) {
        output.put(headerType.displayBytes)
        output.putShort(colonSpaceShort)

        if (value > 1000000000) output.put((48 + (value.rem(10000000000) / 1000000000)).toByte())
        if (value > 100000000) output.put((48 + (value.rem(1000000000) / 100000000)).toByte())
        if (value > 10000000) output.put((48 + (value.rem(100000000) / 10000000)).toByte())
        if (value > 1000000) output.put((48 + (value.rem(10000000) / 1000000)).toByte())
        if (value > 100000) output.put((48 + (value.rem(1000000) / 100000)).toByte())
        if (value > 10000) output.put((48 + (value.rem(100000) / 10000)).toByte())
        if (value > 1000) output.put((48 + (value.rem(10000) / 1000)).toByte())
        if (value > 100) output.put((48 + (value.rem(1000) / 100)).toByte())
        if (value > 10) output.put((48 + (value.rem(100) / 10)).toByte())
        output.put((48 + value.rem(10)).toByte())

        output.putShort(carriageReturnNewLineShort)
    }
}

class ByteArrayResponseHeaderValue(override val value: ByteArray) : HttpResponseHeaderValue<ByteArray>(value) {
    override val valueLength: Int = value.size

    override fun writeHeader(headerType: HttpResponseHeader,
                             output: ByteBuffer) {
        output.put(headerType.displayBytes)
        output.putShort(colonSpaceShort)
        output.put(value)
        output.putShort(carriageReturnNewLineShort)
    }
}

