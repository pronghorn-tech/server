package tech.pronghorn.http.protocol

import tech.pronghorn.util.finder.ByteBacked
import tech.pronghorn.util.finder.ByteBackedFinder
import tech.pronghorn.util.finder.FinderGenerator

interface HttpRequestHeader : ByteBacked {
    fun getHeaderName(): String
}

class InstanceHttpRequestHeader(private val value: AsciiString) : HttpRequestHeader {
    constructor(name: String) : this(AsciiString(name))

    override val bytes = ByteArray(0)

    override fun getHeaderName(): String = value.toString()
}

enum class StandardHttpRequestHeaders(val displayName: String) : HttpRequestHeader {
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

    val parseName: String = displayName.toLowerCase()
    val displayBytes: ByteArray = displayName.toByteArray(Charsets.US_ASCII)
    override val bytes: ByteArray = parseName.toByteArray()

    override fun getHeaderName(): String = displayName

    companion object : ByteBackedFinder<HttpRequestHeader> by standardHeaderFinder
}

private val standardHeaderFinder = FinderGenerator.generateFinder(StandardHttpRequestHeaders.values())
