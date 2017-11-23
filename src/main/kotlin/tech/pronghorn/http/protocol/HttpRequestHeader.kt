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

public interface HttpRequestHeader : ByteBacked {
    public fun getHeaderName(): String
}

public class InstanceHttpRequestHeader(private val value: ByteArray) : HttpRequestHeader {
    constructor(name: String) : this(name.toByteArray(Charsets.US_ASCII))

    override val bytes = ByteArray(0)

    override fun getHeaderName(): String = value.toString()
}

public enum class StandardHttpRequestHeaders(private val displayName: String) : HttpRequestHeader {
    Accept("Accept"),
    AcceptCharset("Accept-Charset"),
    AcceptEncoding("Accept-Encoding"),
    AcceptLanguage("Accept-Language"),
    AcceptDatetime("Accept-Datetime"),
    Authorization("Authorization"),
    CacheControl("Cache-Control"),
    Connection("Connection"),
    Cookie("Cookie"),
    ContentLength("Content-Length"),
    ContentMD5("Content-MD5"),
    ContentType("Content-Type"),
    Date("Date"),
    Expect("Expect"),
    Forwarded("Forwarded"),
    From("From"),
    Host("Host"),
    IfMatch("If-Match"),
    IfModifiedSince("If-Modified-Since"),
    IfNoneMatch("If-None-Match"),
    IfRange("If-Range"),
    IfUnmodifiedSince("If-Unmodified-Since"),
    MaxForwards("Max-Forwards"),
    Origin("Origin"),
    Pragma("Pragma"),
    ProxyAuthorization("Proxy-Authorization"),
    Range("Range"),
    Referer("Referer"),
    TE("TE"),
    UserAgent("User-Agent"),
    Upgrade("Upgrade"),
    UpgradeInsecureRequests("Upgrade-Insecure-Requests"),
    Via("Via"),
    Warning("Warning");

    public val parseName: String = displayName.toLowerCase()
    public val displayBytes: ByteArray = displayName.toByteArray(Charsets.US_ASCII)
    override val bytes: ByteArray = parseName.toByteArray()

    override fun getHeaderName(): String = displayName

    companion object : ByteBackedFinder<HttpRequestHeader> by standardHeaderFinder
}

private val standardHeaderFinder = FinderGenerator.generateFinder(StandardHttpRequestHeaders.values())
