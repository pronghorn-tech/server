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

import java.nio.ByteBuffer
import java.nio.charset.Charset

sealed class ResponseContent {
    companion object {
        fun from(byteArray: ByteArray): ResponseContent = ByteArrayResponseContent(byteArray)

        fun from(arrays: Collection<ByteArray>): ResponseContent = ByteArrayCollectionResponseContent(arrays)

        fun from(string: String,
                 charset: Charset): ResponseContent = StringResponseContent(string, charset)
    }

    abstract val size: Int

    abstract fun writeBody(buffer: ByteBuffer)
}

object EmptyResponseContent: ResponseContent() {
    override val size = 0

    override fun writeBody(buffer: ByteBuffer) = Unit
}

class ByteArrayResponseContent(private val body: ByteArray): ResponseContent() {
    override val size = body.size

    override fun writeBody(buffer: ByteBuffer) {
        if (body.isNotEmpty()) {
            buffer.put(body, 0, body.size)
        }
    }
}

class ByteArrayCollectionResponseContent(private val arrays: Collection<ByteArray>): ResponseContent() {
    override val size = arrays.map { it.size }.sum()

    override fun writeBody(buffer: ByteBuffer) {
        arrays.forEach { bytes ->
            buffer.put(bytes, 0, bytes.size)
        }
    }
}

class StringResponseContent(private val body: String,
                            private val charset: Charset): ResponseContent() {
    private val bytes = body.toByteArray(charset)
    override val size = bytes.size

    override fun writeBody(buffer: ByteBuffer) {
        buffer.put(bytes, 0, bytes.size)
    }
}
