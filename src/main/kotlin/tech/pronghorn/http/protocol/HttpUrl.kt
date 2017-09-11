package tech.pronghorn.http.protocol

import java.net.URLDecoder
import java.util.Arrays
import java.util.Objects

data class QueryParam(val name: AsciiString,
                      val value: AsciiString) {
    constructor(name: String,
                value: String) : this(AsciiString(name), AsciiString(value))
}

sealed class HttpUrlParseResult

object InvalidHttpUrl : HttpUrlParseResult()

object IncompleteHttpUrl : HttpUrlParseResult()

sealed class HttpUrl : HttpUrlParseResult() {
    abstract fun getPathBytes(): ByteArray
    abstract fun getPath(): String
    abstract fun isSecure(): Boolean?
    abstract fun getHostBytes(): ByteArray?
    abstract fun getHost(): String?
    abstract fun getPort(): Int?
    abstract fun getQueryParams(): List<QueryParam>

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is HttpUrl -> {
                return Arrays.equals(getPathBytes(), other.getPathBytes()) &&
                        isSecure() == other.isSecure() &&
                        Arrays.equals(getHostBytes(), other.getHostBytes()) &&
                        getPort() == other.getPort() &&
                        getQueryParams() == other.getQueryParams()
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(getPath(), isSecure(), getHost(), getPort(), getQueryParams())
    }

    override fun toString(): String {
        return "[path='${getPath()}',isSecure=${isSecure()},host='${getHost()}',port=${getPort()},queryParams='${getQueryParams()}']"
    }
}

private val rootBytes = byteArrayOf(forwardSlashByte)

class StringLocationHttpUrl(private val path: AsciiString?,
                            private val isSecure: Boolean? = null,
                            private val host: AsciiString? = null,
                            private val port: Int? = null,
                            private val queryParams: AsciiString? = null,
                            private val pathContainsPercentEncoding: Boolean) : HttpUrl() {
    override fun getPathBytes(): ByteArray {
        if (path == null) {
            return rootBytes
        }
        else {
            return path.bytes
        }
    }

    override fun getPath(): String {
        if (path == null) {
            return "/"
        }
        val pathString = path.toString()
        if (!pathContainsPercentEncoding) {
            return pathString
        }
        else {
            return URLDecoder.decode(pathString, Charsets.UTF_8.name())
        }
    }

    override fun isSecure(): Boolean? = isSecure

    override fun getHostBytes(): ByteArray? = host?.bytes

    override fun getHost(): String? {
        if (host == null) {
            return null
        }

        return String(host.bytes, Charsets.US_ASCII)
    }

    override fun getPort(): Int? = port

    override fun getQueryParams(): List<QueryParam> {
        if(queryParams == null) {
            return emptyList()
        }

        val params = mutableListOf<QueryParam>()
        var x = 0
        var nameStart = 0
        var valueStart = -1
        while(x < queryParams.bytes.size){
            val byte = queryParams.bytes[x]
            if(byte == equalsByte){
                valueStart = x + 1
            }
            else if(byte == ampersandByte){
                if(valueStart != -1){
                    val name = AsciiString(queryParams.bytes, nameStart, valueStart - nameStart - 1)
                    val value = AsciiString(queryParams.bytes, valueStart, x - valueStart)
                    params.add(QueryParam(name, value))
                }
                nameStart = x + 1
                valueStart = -1
            }
            x += 1
        }

        if(valueStart != -1){
            val name = AsciiString(queryParams.bytes, nameStart, valueStart - nameStart - 1)
            val value = AsciiString(queryParams.bytes, valueStart, x - valueStart)
            params.add(QueryParam(name, value))
        }

        return params
    }
}

class ValueHttpUrl(private val path: String,
                   private val containsPercentEncoding: Boolean = false,
                   private val isSecure: Boolean? = null,
                   private val host: String? = null,
                   private val port: Int? = null,
                   private val queryParams: List<QueryParam> = emptyList()) : HttpUrl() {

    override fun getPathBytes(): ByteArray = path.toByteArray(Charsets.US_ASCII)

    override fun getPath(): String {
        if (!containsPercentEncoding) {
            return path
        }
        else {
            return URLDecoder.decode(path, Charsets.UTF_8.name())
        }
    }

    override fun isSecure(): Boolean? = isSecure

    override fun getHostBytes(): ByteArray? = host?.toByteArray(Charsets.US_ASCII)

    override fun getHost(): String? = host

    override fun getPort(): Int? = port

    override fun getQueryParams(): List<QueryParam> = queryParams
}
