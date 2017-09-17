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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.pronghorn.http.protocol.*
import tech.pronghorn.test.PronghornTest
import java.nio.ByteBuffer

data class ParseTest(val uriString: String,
                     val uri: HttpUrl) {
    val bytes = uriString.toByteArray(Charsets.US_ASCII)

    fun fitBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.put(bytes)
        buffer.flip()
        return buffer
    }

    fun beginningSpaceBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(bytes.size + 2)
        buffer.put(spaceByte)
        buffer.put(spaceByte)
        buffer.put(bytes)
        buffer.flip()
        buffer.position(2)
        return buffer
    }

    fun endingSpaceBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(bytes.size + 2)
        buffer.put(bytes)
        buffer.put(spaceByte)
        buffer.put(spaceByte)
        buffer.flip()
        return buffer
    }

    // parses the test's url with extra spaces before and after to ensure correct buffer use
    fun execute() {
        val parsedA = parseHttpUrl(fitBuffer())
        Assertions.assertEquals(uri, parsedA)

        val parsedB = parseHttpUrl(beginningSpaceBuffer())
        Assertions.assertEquals(uri, parsedB)

        val parsedC = parseHttpUrl(endingSpaceBuffer())
        Assertions.assertEquals(uri, parsedC)
    }
}

class HttpUrlParseTests : PronghornTest() {
    val testUrls = listOf(
            "/",
            "*",
            "/foo",
            "http://example.com/",
            "https://example.com:5000/foo",
            "/foo?bar=5&baz=potato"
    )

    @Test
    /*
     * Ensures no partial url parsing throws an exception
     */
    fun progressiveUrlParseTest() {
        testUrls.forEach { testUrl ->
            var x = 0
            while (x < testUrl.length) {
                val bytes = testUrl.toByteArray(Charsets.US_ASCII)
                val buffer = ByteBuffer.allocateDirect(bytes.size)
                buffer.put(bytes)
                parseHttpUrl(buffer)
                x += 1
            }
        }
    }

    @Test
    fun urlParseTest1() {
        val test = ParseTest(
                "/",
                ValueHttpUrl(path = "/")
        )
        test.execute()
    }

    @Test
    fun urlParseTest2() {
        val test = ParseTest(
                "*",
                ValueHttpUrl(path = "*")
        )
        test.execute()
    }

    @Test
    fun urlParseTest3() {
        val test = ParseTest(
                "/foo",
                ValueHttpUrl(path = "/foo")
        )
        test.execute()
    }

    @Test
    fun urlParseTest4() {
        val test = ParseTest(
                "http://example.com",
                ValueHttpUrl(path = "/", isSecure = false, host = "example.com")
        )
        test.execute()
    }

    @Test
    fun urlParseTest5() {
        val test = ParseTest(
                "http://example.com/",
                ValueHttpUrl(path = "/", isSecure = false, host = "example.com")
        )
        test.execute()
    }

    @Test
    fun urlParseTest6() {
        val test = ParseTest(
                "https://example.com",
                ValueHttpUrl(path = "/", isSecure = true, host = "example.com")
        )
        test.execute()
    }

    @Test
    fun urlParseTest7() {
        val test = ParseTest(
                "https://example.com:10",
                ValueHttpUrl(path = "/", isSecure = true, host = "example.com", port = 10)
        )
        test.execute()
    }

    @Test
    fun urlParseTest8() {
        val test = ParseTest(
                "https://example.com:4000/",
                ValueHttpUrl(path = "/", isSecure = true, host = "example.com", port = 4000)
        )
        test.execute()
    }

    @Test
    fun urlParseTest9() {
        val test = ParseTest(
                "https://example.com:5000/foo",
                ValueHttpUrl(path = "/foo", isSecure = true, host = "example.com", port = 5000)
        )
        test.execute()
    }

    @Test
    fun urlParseTest10() {
        val test = ParseTest(
                "/foo?bar=5",
                ValueHttpUrl(path = "/foo", queryParams = listOf(QueryParam("bar", "5")))
        )
        test.execute()
    }

    @Test
    fun urlParseTest11() {
        val test = ParseTest(
                "/foo?bar=5&baz=potato",
                ValueHttpUrl(path = "/foo", queryParams = listOf(QueryParam("bar", "5"), QueryParam("baz", "potato")))
        )
        test.execute()
    }

    @Test
    fun urlParseTest12() {
        val test = ParseTest(
                "/foo?bar=5&baz=potato&buzz=bingo",
                ValueHttpUrl(path = "/foo", queryParams = listOf(
                        QueryParam("bar", "5"),
                        QueryParam("baz", "potato"),
                        QueryParam("buzz", "bingo")
                ))
        )
        test.execute()
    }

    // TODO: punycode test

    // TODO: percent encoding test
}
