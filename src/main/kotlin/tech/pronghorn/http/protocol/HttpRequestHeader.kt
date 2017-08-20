package tech.pronghorn.http.protocol

import tech.pronghorn.http.AsciiString
import tech.pronghorn.util.finder.ByteBacked
import tech.pronghorn.util.finder.ByteBackedFinder
import tech.pronghorn.util.Either
import tech.pronghorn.util.finder.FinderGenerator

//data class HttpRequestHeader(val headerName: String,
//                              val bytes: ByteArray) {
//    companion object {
//        val Accept = HttpRequestHeader("accept", "accept".toByteArray(Charsets.US_ASCII))
//        val AcceptCharset = HttpRequestHeader("accept-charset", "accept-charset".toByteArray(Charsets.US_ASCII))
//        val AcceptEncoding = HttpRequestHeader("accept-encoding", "accept-encoding".toByteArray(Charsets.US_ASCII))
//        val AcceptLanguage = HttpRequestHeader("accept-language", "accept-language".toByteArray(Charsets.US_ASCII))
//        val AcceptDatetime = HttpRequestHeader("accept-datetime", "accept-datetime".toByteArray(Charsets.US_ASCII))
//        val Authorization = HttpRequestHeader("authorization", "authorization".toByteArray(Charsets.US_ASCII))
//        val CacheControl = HttpRequestHeader("cache-control", "cache-control".toByteArray(Charsets.US_ASCII))
//        val Connection = HttpRequestHeader("connection", "connection".toByteArray(Charsets.US_ASCII))
//        val Cookie = HttpRequestHeader("cookie", "cookie".toByteArray(Charsets.US_ASCII))
//        val ContentLength = HttpRequestHeader("content-length", "content-length".toByteArray(Charsets.US_ASCII))
//        val ContentMD5 = HttpRequestHeader("content-md5", "content-md5".toByteArray(Charsets.US_ASCII))
//        val ContentType = HttpRequestHeader("content-type", "content-type".toByteArray(Charsets.US_ASCII))
//        val Date = HttpRequestHeader("date", "date".toByteArray(Charsets.US_ASCII))
//        val Expect = HttpRequestHeader("expect", "expect".toByteArray(Charsets.US_ASCII))
//        val Forwarded = HttpRequestHeader("forwarded", "forwarded".toByteArray(Charsets.US_ASCII))
//        val From = HttpRequestHeader("from", "from".toByteArray(Charsets.US_ASCII))
//        val Host = HttpRequestHeader("host", "host".toByteArray(Charsets.US_ASCII))
//        val IfMatch = HttpRequestHeader("if-match", "if-match".toByteArray(Charsets.US_ASCII))
//        val IfModifiedSince = HttpRequestHeader("if-modified-since", "if-modified-since".toByteArray(Charsets.US_ASCII))
//        val IfNoneMatch = HttpRequestHeader("if-none-match", "if-none-match".toByteArray(Charsets.US_ASCII))
//        val IfRange = HttpRequestHeader("if-range", "if-range".toByteArray(Charsets.US_ASCII))
//        val IfUnmodifiedSince = HttpRequestHeader("if-unmodified-since", "if-unmodified-since".toByteArray(Charsets.US_ASCII))
//        val MaxForwards = HttpRequestHeader("max-forwards", "max-forwards".toByteArray(Charsets.US_ASCII))
//        val Origin = HttpRequestHeader("origin", "origin".toByteArray(Charsets.US_ASCII))
//        val Pragma = HttpRequestHeader("pragma", "pragma".toByteArray(Charsets.US_ASCII))
//        val ProxyAuthorization = HttpRequestHeader("proxy-authorization", "proxy-authorization".toByteArray(Charsets.US_ASCII))
//        val Range = HttpRequestHeader("range", "range".toByteArray(Charsets.US_ASCII))
//        val Referer = HttpRequestHeader("referer", "referer".toByteArray(Charsets.US_ASCII))
//        val TE = HttpRequestHeader("te", "te".toByteArray(Charsets.US_ASCII))
//        val UserAgent = HttpRequestHeader("user-agent", "user-agent".toByteArray(Charsets.US_ASCII))
//        val Upgrade = HttpRequestHeader("upgrade", "upgrade".toByteArray(Charsets.US_ASCII))
//        val UpgradeInsecureRequests = HttpRequestHeader("upgrade-insecure-requests", "upgrade-insecure-requests".toByteArray(Charsets.US_ASCII))
//        val Via = HttpRequestHeader("via", "via".toByteArray(Charsets.US_ASCII))
//        val Warning = HttpRequestHeader("warning", "warning".toByteArray(Charsets.US_ASCII))
//
//        private val values = arrayOf(
//                Accept,
//                AcceptCharset,
//                AcceptEncoding,
//                AcceptLanguage,
//                AcceptDatetime,
//                Authorization,
//                CacheControl,
//                Connection,
//                Cookie,
//                ContentLength,
//                ContentMD5,
//                ContentType,
//                Date,
//                Expect,
//                Forwarded,
//                From,
//                Host,
//                IfMatch,
//                IfModifiedSince,
//                IfNoneMatch,
//                IfRange,
//                IfUnmodifiedSince,
//                MaxForwards,
//                Origin,
//                Pragma,
//                ProxyAuthorization,
//                Range,
//                Referer,
//                TE,
//                UserAgent,
//                Upgrade,
//                UpgradeInsecureRequests,
//                Via,
//                Warning
//        )
//
//        private val maxLength = values.map { method -> method.bytes.size }.max() ?: 0
//        val byLength = arrayOfNulls<Array<HttpRequestHeader>>(maxLength + 1)
//        init {
//            var x = 0
//            while(x < byLength.size){
//                byLength[x] = values.filter { method -> method.bytes.size == x }.toTypedArray()
//                x += 1
//            }
//        }
//    }
//}

//
//object HttpRequestHeaders {
//    val Accept = HttpRequestHeader1("accept", "accept".toByteArray(Charsets.US_ASCII))
//    val AcceptCharset = HttpRequestHeader1("accept-charset", "accept-charset".toByteArray(Charsets.US_ASCII))
//    val AcceptEncoding = HttpRequestHeader1("accept-encoding", "accept-encoding".toByteArray(Charsets.US_ASCII))
//    val AcceptLanguage = HttpRequestHeader1("accept-language", "accept-language".toByteArray(Charsets.US_ASCII))
//    val AcceptDatetime = HttpRequestHeader1("accept-datetime", "accept-datetime".toByteArray(Charsets.US_ASCII))
//    val Authorization = HttpRequestHeader1("authorization", "authorization".toByteArray(Charsets.US_ASCII))
//    val CacheControl = HttpRequestHeader1("cache-control", "cache-control".toByteArray(Charsets.US_ASCII))
//    val Connection = HttpRequestHeader1("connection", "connection".toByteArray(Charsets.US_ASCII))
//    val Cookie = HttpRequestHeader1("cookie", "cookie".toByteArray(Charsets.US_ASCII))
//    val ContentLength = HttpRequestHeader1("content-length", "content-length".toByteArray(Charsets.US_ASCII))
//    val ContentMD5 = HttpRequestHeader1("content-md5", "content-md5".toByteArray(Charsets.US_ASCII))
//    val ContentType = HttpRequestHeader1("content-type", "content-type".toByteArray(Charsets.US_ASCII))
//    val Date = HttpRequestHeader1("date", "date".toByteArray(Charsets.US_ASCII))
//    val Expect = HttpRequestHeader1("expect", "expect".toByteArray(Charsets.US_ASCII))
//    val Forwarded = HttpRequestHeader1("forwarded", "forwarded".toByteArray(Charsets.US_ASCII))
//    val From = HttpRequestHeader1("from", "from".toByteArray(Charsets.US_ASCII))
//    val Host = HttpRequestHeader1("host", "host".toByteArray(Charsets.US_ASCII))
//    val IfMatch = HttpRequestHeader1("if-match", "if-match".toByteArray(Charsets.US_ASCII))
//    val IfModifiedSince = HttpRequestHeader1("if-modified-since", "if-modified-since".toByteArray(Charsets.US_ASCII))
//    val IfNoneMatch = HttpRequestHeader1("if-none-match", "if-none-match".toByteArray(Charsets.US_ASCII))
//    val IfRange = HttpRequestHeader1("if-range", "if-range".toByteArray(Charsets.US_ASCII))
//    val IfUnmodifiedSince = HttpRequestHeader1("if-unmodified-since", "if-unmodified-since".toByteArray(Charsets.US_ASCII))
//    val MaxForwards = HttpRequestHeader1("max-forwards", "max-forwards".toByteArray(Charsets.US_ASCII))
//    val Origin = HttpRequestHeader1("origin", "origin".toByteArray(Charsets.US_ASCII))
//    val Pragma = HttpRequestHeader1("pragma", "pragma".toByteArray(Charsets.US_ASCII))
//    val ProxyAuthorization = HttpRequestHeader1("proxy-authorization", "proxy-authorization".toByteArray(Charsets.US_ASCII))
//    val Range = HttpRequestHeader1("range", "range".toByteArray(Charsets.US_ASCII))
//    val Referer = HttpRequestHeader1("referer", "referer".toByteArray(Charsets.US_ASCII))
//    val TE = HttpRequestHeader1("te", "te".toByteArray(Charsets.US_ASCII))
//    val UserAgent = HttpRequestHeader1("user-agent", "user-agent".toByteArray(Charsets.US_ASCII))
//    val Upgrade = HttpRequestHeader1("upgrade", "upgrade".toByteArray(Charsets.US_ASCII))
//    val UpgradeInsecureRequests = HttpRequestHeader1("upgrade-insecure-requests", "upgrade-insecure-requests".toByteArray(Charsets.US_ASCII))
//    val Via = HttpRequestHeader1("via", "via".toByteArray(Charsets.US_ASCII))
//    val Warning = HttpRequestHeader1("warning", "warning".toByteArray(Charsets.US_ASCII))
//
//    private val values = arrayOf(
//            Accept,
//            AcceptCharset,
//            AcceptEncoding,
//            AcceptLanguage,
//            AcceptDatetime,
//            Authorization,
//            CacheControl,
//            Connection,
//            Cookie,
//            ContentLength,
//            ContentMD5,
//            ContentType,
//            Date,
//            Expect,
//            Forwarded,
//            From,
//            Host,
//            IfMatch,
//            IfModifiedSince,
//            IfNoneMatch,
//            IfRange,
//            IfUnmodifiedSince,
//            MaxForwards,
//            Origin,
//            Pragma,
//            ProxyAuthorization,
//            Range,
//            Referer,
//            TE,
//            UserAgent,
//            Upgrade,
//            UpgradeInsecureRequests,
//            Via,
//            Warning
//    )
//
//    private val maxLength = values.map { method -> method.bytes.size }.max() ?: 0
//    val byLength = arrayOfNulls<Array<HttpRequestHeader1>>(maxLength + 1)
//    init {
//        var x = 0
//        while(x < byLength.size){
//            byLength[x] = values.filter { method -> method.bytes.size == x }.toTypedArray()
//            x += 1
//        }
//    }
//}
//
interface HttpRequestHeader: ByteBacked {
    fun getHeaderName(): String
//    fun getBytes(): ByteArray
}

class CustomHttpRequestHeader(private val value: Either<AsciiString, String>) : HttpRequestHeader {
    constructor(location: AsciiString): this(Either.Left(location))

    constructor(name: String): this(Either.Right(name))

    override val bytes = ByteArray(0)

    override fun getHeaderName(): String {
        when(value) {
            is Either.Left -> return value.value.toString()
            is Either.Right -> return value.value
        }
    }

//    override fun getBytes(): ByteArray {
//        when(value) {
//            is Either.Left -> TODO() //return value.value.toByteArray()
//            is Either.Right -> return value.value.toByteArray(Charsets.US_ASCII)
//        }
//    }
}

enum class StandardHttpRequestHeaders(val _headerName: String,
                                      override val bytes: ByteArray) : HttpRequestHeader {
    Accept("accept", "accept".toByteArray(Charsets.US_ASCII)),
    AcceptCharset("accept-charset", "accept-charset".toByteArray(Charsets.US_ASCII)),
    AcceptEncoding("accept-encoding", "accept-encoding".toByteArray(Charsets.US_ASCII)),
    AcceptLanguage("accept-language", "accept-language".toByteArray(Charsets.US_ASCII)),
    AcceptDatetime("accept-datetime", "accept-datetime".toByteArray(Charsets.US_ASCII)),
    Authorization("authorization", "authorization".toByteArray(Charsets.US_ASCII)),
    CacheControl("cache-control", "cache-control".toByteArray(Charsets.US_ASCII)),
    Connection("connection", "connection".toByteArray(Charsets.US_ASCII)),
    Cookie("cookie", "cookie".toByteArray(Charsets.US_ASCII)),
    ContentLength("content-length", "content-length".toByteArray(Charsets.US_ASCII)),
    ContentMD5("content-md5", "content-md5".toByteArray(Charsets.US_ASCII)),
    ContentType("content-type", "content-type".toByteArray(Charsets.US_ASCII)),
    Date("date", "date".toByteArray(Charsets.US_ASCII)),
    Expect("expect", "expect".toByteArray(Charsets.US_ASCII)),
    Forwarded("forwarded", "forwarded".toByteArray(Charsets.US_ASCII)),
    From("from", "from".toByteArray(Charsets.US_ASCII)),
    Host("host", "host".toByteArray(Charsets.US_ASCII)),
    IfMatch("if-match", "if-match".toByteArray(Charsets.US_ASCII)),
    IfModifiedSince("if-modified-since", "if-modified-since".toByteArray(Charsets.US_ASCII)),
    IfNoneMatch("if-none-match", "if-none-match".toByteArray(Charsets.US_ASCII)),
    IfRange("if-range", "if-range".toByteArray(Charsets.US_ASCII)),
    IfUnmodifiedSince("if-unmodified-since", "if-unmodified-since".toByteArray(Charsets.US_ASCII)),
    MaxForwards("max-forwards", "max-forwards".toByteArray(Charsets.US_ASCII)),
    Origin("origin", "origin".toByteArray(Charsets.US_ASCII)),
    Pragma("pragma", "pragma".toByteArray(Charsets.US_ASCII)),
    ProxyAuthorization("proxy-authorization", "proxy-authorization".toByteArray(Charsets.US_ASCII)),
    Range("range", "range".toByteArray(Charsets.US_ASCII)),
    Referer("referer", "referer".toByteArray(Charsets.US_ASCII)),
    TE("te", "te".toByteArray(Charsets.US_ASCII)),
    UserAgent("user-agent", "user-agent".toByteArray(Charsets.US_ASCII)),
    Upgrade("upgrade", "upgrade".toByteArray(Charsets.US_ASCII)),
    UpgradeInsecureRequests("upgrade-insecure-requests", "upgrade-insecure-requests".toByteArray(Charsets.US_ASCII)),
    Via("via", "via".toByteArray(Charsets.US_ASCII)),
    Warning("warning", "warning".toByteArray(Charsets.US_ASCII));

    override fun getHeaderName(): String = _headerName

    companion object: ByteBackedFinder<StandardHttpRequestHeaders> by standardHeaderFinder {
        fun registerHeader(header: CustomHttpRequestHeader){
            TODO()
        }
    }
}

private val standardHeaderFinder = FinderGenerator.generateFinder(StandardHttpRequestHeaders.values())
