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
