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
import tech.pronghorn.util.*
import java.nio.ByteBuffer

public sealed class HttpResponseHeaderValue<T> {
    companion object {
        public fun valueOf(value: Int): HttpResponseHeaderValue<Int> = IntResponseHeaderValue(value)

        public fun valueOf(value: Long): HttpResponseHeaderValue<Long> = LongResponseHeaderValue(value)

        public fun valueOf(value: String): HttpResponseHeaderValue<ByteArray> = ByteArrayResponseHeaderValue(value.toByteArray(Charsets.US_ASCII))

        public fun valueOf(value: ByteArray): HttpResponseHeaderValue<ByteArray> = ByteArrayResponseHeaderValue(value)
    }

    public abstract val value: T
    public abstract val valueLength: Int

    public fun writeHeader(headerType: HttpResponseHeader,
                    buffer: ByteBuffer) {
        buffer.put(headerType.displayBytes)
        buffer.putShort(colonSpaceShort)
        writeHeaderValue(buffer)
        buffer.putShort(carriageReturnNewLineShort)
    }

    public abstract fun writeHeaderValue(buffer: ByteBuffer)
}

public class IntResponseHeaderValue(override val value: Int) : HttpResponseHeaderValue<Int>() {
    override val valueLength = stringLengthOfInt(value)

    override fun writeHeaderValue(buffer: ByteBuffer) = writeIntAsStringToBuffer(value, buffer)
}

public class LongResponseHeaderValue(override val value: Long) : HttpResponseHeaderValue<Long>() {
    override val valueLength = stringLengthOfLong(value)

    override fun writeHeaderValue(buffer: ByteBuffer) = writeLongAsStringToBuffer(value, buffer)
}

public class ByteArrayResponseHeaderValue(override val value: ByteArray) : HttpResponseHeaderValue<ByteArray>() {
    override val valueLength: Int = value.size

    override fun writeHeaderValue(buffer: ByteBuffer) {
        buffer.put(value)
    }
}

