package tech.pronghorn.http.protocol

import tech.pronghorn.http.AsciiString
import tech.pronghorn.server.*
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.util.*

data class QueryParam(val name: AsciiString,
                      val value: AsciiString)

data class RequestCredentials(val username: AsciiString,
                              val password: AsciiString)

val RootURI = ValueHttpRequestURI("/")
val StarURI = ValueHttpRequestURI("*")

const private val httpAsInt = 1752462448
const private val doubleSlashAsShort: Short = 12079

fun parseHttpURI(buffer: ByteBuffer): HttpRequestURI {
    var byte = buffer.get()

    var pathContainsPercentEncoding = false
    var credentialsStart = -1
    var pathStart = -1
    var portStart = -1
    var hostStart = -1
    var queryParamStart = -1
    var end = -1
    var port: Int? = null
    var isSecure: Boolean? = null

    if (byte == forwardSlashByte) {
        if(!buffer.hasRemaining()){
            return RootURI
        }

        pathStart = buffer.position() - 1
        // abs_path
        while (buffer.hasRemaining()) {
            byte = buffer.get()

            if (byte == percentByte) {
                pathContainsPercentEncoding = true
            }
            else if (byte == questionByte) {
                queryParamStart = buffer.position()
            }
            else if (byte == spaceByte) {
                end = buffer.position() - 1
                if(end - pathStart == 1){
                    return RootURI
                }
                break
            }
        }
    }
    else if(byte == asteriskByte){
        // starURI
        if(!buffer.hasRemaining() || buffer.get() == spaceByte){
            return StarURI
        }
        else {
            TODO("Exception")
        }
    }
    else {
        buffer.position(buffer.position() - 1)
        // absoluteURI
        if (buffer.remaining() < 4) {
            TODO("Exception")
        }

        val firstFour = buffer.getInt()
        if (firstFour != httpAsInt) {
            TODO("Exception")
        }

        val secureByte: Byte = 0x73

        byte = buffer.get()
        isSecure = byte == secureByte

        if(isSecure){
            byte = buffer.get()
        }

        if (byte == colonByte) {
            if (buffer.remaining() < 2) {
                TODO("Exception")
            }
            val slashes = buffer.getShort()
            if (slashes != doubleSlashAsShort) {
                TODO("Exception")
            }
        }
        else {
            TODO("Exception")
        }

        hostStart = buffer.position()

        while (buffer.hasRemaining()) {
            byte = buffer.get()

            if (byte == colonByte) {
                // parse port
                portStart = buffer.position()
                port = 0
                while (buffer.hasRemaining()) {
                    val portByte = buffer.get()
                    if (portByte == forwardSlashByte) {
                        pathStart = buffer.position() - 1
                        break
                    }
                    else if(portByte == spaceByte) {
                        end = buffer.position() - 1
                        break
                    }

                    port = port!! * 10 + (portByte - 48)
                }
                break
            }
            else if (byte == forwardSlashByte) {
                pathStart = buffer.position() - 1
                break
            }
            else if (byte == spaceByte) {
                end = buffer.position() - 1
                break
            }
        }

        while (buffer.hasRemaining()) {
            byte = buffer.get()
            if (byte == percentByte) {
                pathContainsPercentEncoding = true
            }
            else if (byte == questionByte) {
                queryParamStart = buffer.position()
                break
            }
            else if (byte == atByte) {
                if (!isSecure) {
                    TODO("Exception")
                }

                credentialsStart = pathStart
                pathStart = buffer.position()
            }
            else if (byte == spaceByte) {
                end = buffer.position() - 1
                break
            }
        }
    }

    if(end == -1){
        end = buffer.position()
    }

    val path = if (pathStart != -1) {
        AsciiString(buffer, pathStart, end - pathStart)
    }
    else {
        null
    }

    val prePath = if (pathStart != -1) {
        pathStart
    }
    else {
        end
    }

    val credentials = if (credentialsStart != -1) {
        AsciiString(buffer, credentialsStart, prePath - credentialsStart)
    }
    else {
        null
    }

    val host = if (hostStart != -1) {
        AsciiString(buffer, hostStart, if (portStart != -1) portStart - 1 - hostStart else prePath - hostStart)
    }
    else {
        null
    }

    val queryParams = if (queryParamStart != -1) {
        AsciiString(buffer, queryParamStart, end - queryParamStart)
    }
    else {
        null
    }

    return StringLocationHttpRequestURI(
            path = path,
            credentials = credentials,
            isSecure = isSecure,
            host = host,
            port = port,
            queryParams = queryParams,
            pathContainsPercentEncoding = pathContainsPercentEncoding
    )
}

abstract class HttpRequestURI {
    abstract fun getPathBytes(): ByteArray
    abstract fun getPath(): String
    abstract fun isSecure(): Boolean?
    abstract fun getCredentials(): RequestCredentials?
    abstract fun getHostBytes(): ByteArray?
    abstract fun getHost(): String?
    abstract fun getPort(): Int?
    abstract fun getQueryParams(): List<QueryParam>?

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is HttpRequestURI -> {
                return Arrays.equals(getPathBytes(), other.getPathBytes()) &&
                        isSecure() == other.isSecure() &&
                        getCredentials() == other.getCredentials() &&
                        Arrays.equals(getHostBytes(), other.getHostBytes()) &&
                        getPort() == other.getPort() &&
                        getQueryParams() == other.getQueryParams()
            }
            else -> false
        }
    }

    override fun toString(): String {
        return "[path='${getPath()}',isSecure=${isSecure()},credentials='${getCredentials()}',host='${getHost()}',port=${getPort()},queryParams='${getQueryParams()}']"
    }
}

class StringLocationHttpRequestURI(private val path: AsciiString?,
                                   private val isSecure: Boolean? = null,
                                   private val credentials: AsciiString? = null,
                                   private val host: AsciiString? = null,
                                   private val port: Int? = null,
                                   private val queryParams: AsciiString? = null,
                                   private val pathContainsPercentEncoding: Boolean) : HttpRequestURI() {
//    val pathString by lazy(LazyThreadSafetyMode.NONE) {
//        if (path == null) {
//            "/"
//        }
//        else {
//            val pathString = path.toString()
//            if (!pathContainsPercentEncoding) {
//                pathString
//            } else {
//                URLDecoder.decode(pathString, Charsets.UTF_8.name())
//            }
//        }
//    }

    override fun getPathBytes(): ByteArray {
        if (path == null) {
            return byteArrayOf(forwardSlashByte)
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

    override fun getCredentials(): RequestCredentials? {
        if (credentials == null) {
            return null
        }

        return null
    }

    override fun getHostBytes(): ByteArray? = host?.bytes

    override fun getHost(): String? {
        if (host == null) {
            return null
        }

        return String(host.bytes, Charsets.US_ASCII)
    }

    override fun getPort(): Int? = port

    override fun getQueryParams(): List<QueryParam>? {
        return null
    }
}

class ValueHttpRequestURI(private val path: String,
                          private val containsPercentEncoding: Boolean = false,
                          private val isSecure: Boolean? = null,
                          private val credentials: RequestCredentials? = null,
                          private val host: String? = null,
                          private val port: Int? = null,
                          private val queryParams: List<QueryParam>? = null) : HttpRequestURI() {

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

    override fun getCredentials(): RequestCredentials? = credentials

    override fun getHostBytes(): ByteArray? = host?.toByteArray(Charsets.US_ASCII)

    override fun getHost(): String? = host

    override fun getPort(): Int? = port

    override fun getQueryParams(): List<QueryParam>? = queryParams
}
