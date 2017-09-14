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

import tech.pronghorn.util.finder.ByteBacked

interface HttpResponseHeader : ByteBacked {
    fun getHeaderName(): String

    val displayBytes: ByteArray
}

class InstanceHttpResponseHeader(val displayName: String) : HttpResponseHeader {
    val parseName = displayName.toLowerCase()
    override val displayBytes: ByteArray = displayName.toByteArray(Charsets.US_ASCII)
    override val bytes: ByteArray = parseName.toByteArray(Charsets.US_ASCII)

    override fun getHeaderName(): String = displayName
}

enum class StandardHttpResponseHeaders(val displayName: String) : HttpResponseHeader {
    AccessControlAllowOrigin("Access-Control-Allow-Origin"),
    AcceptPatch("Accept-Patch"),
    AcceptRanges("Accept-Ranges"),
    Age("Age"),
    Allow("Allow"),
    AltSvc("Alt-Svc"),
    CacheControl("Cache-Control"),
    Connection("Connection"),
    ContentDisposition("Content-Disposition"),
    ContentEncoding("Content-Encoding"),
    ContentLanguage("Content-Language"),
    ContentLength("Content-Length"),
    ContentLocation("Content-Location"),
    ContentMD5("Content-MD5"),
    ContentRange("Content-Range"),
    ContentType("Content-Type"),
    Date("Date"),
    ETag("ETag"),
    Expires("Expires"),
    LastModified("Last-Modified"),
    Link("Link"),
    Location("Location"),
    P3P("P3P"),
    Pragma("Pragma"),
    ProxyAuthenticate("Proxy-Authenticate"),
    PublicKeyPins("Public-Key-Pins"),
    Refresh("Refresh"),
    RetryAfter("Retry-After"),
    Server("Server"),
    SetCookie("Set-Cookie"),
    StrictTransportSecurity("Strict-Transport-Security"),
    Trailer("Trailer"),
    TransferEncoding("Transfer-Encoding"),
    Tk("TK"),
    Upgrade("Upgrade"),
    Vary("Vary"),
    Via("Via"),
    Warning("Warning"),
    WWWAuthenticate("WWW-Authenticate"),
    XFrameOptions("X-Frame-Options");

    val parseName: String = displayName.toLowerCase()
    override val displayBytes: ByteArray = displayName.toByteArray(Charsets.US_ASCII)
    override val bytes: ByteArray = parseName.toByteArray(Charsets.US_ASCII)

    override fun getHeaderName(): String = displayName
}
