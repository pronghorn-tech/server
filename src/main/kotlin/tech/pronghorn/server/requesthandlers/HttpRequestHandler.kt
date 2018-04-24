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

package tech.pronghorn.server.requesthandlers

import tech.pronghorn.http.HttpRequest
import tech.pronghorn.http.HttpResponse
import tech.pronghorn.plugins.logging.LoggingPlugin

public sealed class HttpRequestHandler {
    protected val logger = LoggingPlugin.get(javaClass)
}

public abstract class SuspendableHttpRequestHandler: HttpRequestHandler() {
    abstract suspend fun handle(request: HttpRequest): HttpResponse
}

public abstract class NonSuspendableHttpRequestHandler: HttpRequestHandler() {
    abstract fun handle(request: HttpRequest): HttpResponse
}
