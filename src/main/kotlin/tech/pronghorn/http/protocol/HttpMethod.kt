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

package tech.pronghorn.http.protocol

import tech.pronghorn.util.finder.*

enum class HttpMethod(val methodName: String) : ByteBacked {
    CONNECT("CONNECT"),
    DELETE("DELETE"),
    GET("GET"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS"),
    PATCH("PATCH"),
    POST("POST"),
    PUT("PUT"),
    TRACE("TRACE");

    override val bytes: ByteArray = methodName.toByteArray(Charsets.US_ASCII)

    companion object : ByteBackedFinder<HttpMethod> by httpMethodFinder
}

private val httpMethodFinder = FinderGenerator.generateFinder(HttpMethod.values())
