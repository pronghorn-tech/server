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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import tech.pronghorn.http.protocol.StandardHttpResponseHeaders
import tech.pronghorn.test.PronghornTest
import tech.pronghorn.test.lightRepeatCount
import java.nio.ByteBuffer
import java.util.Arrays

class HttpResponseHeaderValueTests: PronghornTest() {
    @Test
    fun numericValueTest() {
        var x = 0
        val buffer = ByteBuffer.allocate(64)
        while(x < 1000000){
            val value = IntResponseHeaderValue(x)
            buffer.clear()
            value.writeHeaderValue(buffer)
            buffer.flip()
            val arr = ByteArray(buffer.limit())
            buffer.get(arr)
            val valueString = String(arr)
            assertEquals(valueString.toInt(), x)
            x += 1
        }
    }

    @RepeatedTest(lightRepeatCount)
    fun byteArrayValueTest() {
        val length = 8 + random.nextInt(32)
        val buffer = ByteBuffer.allocate(length)
        val bytes = ByteArray(length)
        random.nextBytes(bytes)

        val value = ByteArrayResponseHeaderValue(bytes)
        value.writeHeaderValue(buffer)

        buffer.flip()
        val validationBytes = ByteArray(buffer.limit())
        buffer.get(validationBytes)

        assertTrue(Arrays.equals(validationBytes, bytes))
    }

    @Test
    fun fullHeaderTest() {
        val numeric = random.nextInt(256)
        val header = StandardHttpResponseHeaders.ContentLength
        val expected = "${String(header.displayBytes, Charsets.US_ASCII)}: $numeric\r\n"

        val value = IntResponseHeaderValue(numeric)
        val buffer = ByteBuffer.allocate(header.displayBytes.size + 2 + value.valueLength + 2)
        value.writeHeader(StandardHttpResponseHeaders.ContentLength, buffer)
        buffer.flip()
        val bytes = ByteArray(buffer.limit())
        buffer.get(bytes)
        val result = String(bytes, Charsets.US_ASCII)

        assertEquals(result, expected)
    }
}
