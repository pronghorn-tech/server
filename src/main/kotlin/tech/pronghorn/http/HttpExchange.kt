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
import tech.pronghorn.server.HttpServerConnection

sealed class HttpParseResult

object IncompleteRequestParseError : HttpParseResult()

object InvalidUrlParseError : HttpParseResult()

object InvalidMethodParseError : HttpParseResult()

object InvalidVersionParseError : HttpParseResult()

class HttpExchange(val requestMethod: HttpMethod,
                   val requestUrl: HttpUrl,
                   val version: HttpVersion,
                   val requestHeaders: Map<HttpRequestHeader, ByteArray>,
                   val connection: HttpServerConnection,
                   val requestBody: ByteArray?) : HttpParseResult()


