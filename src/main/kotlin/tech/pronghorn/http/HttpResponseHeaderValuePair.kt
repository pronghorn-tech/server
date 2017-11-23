package tech.pronghorn.http

import tech.pronghorn.http.protocol.HttpResponseHeader
import java.nio.ByteBuffer

public class HttpResponseHeaderValuePair(public val type: HttpResponseHeader,
                                         public val value: HttpResponseHeaderValue<*>) {
    public val length = type.displayBytes.size + value.valueLength + 4 // "header: value\r\n"

    constructor(headerType: HttpResponseHeader,
                value: ByteArray) : this(headerType, HttpResponseHeaderValue.valueOf(value))

    constructor(headerType: HttpResponseHeader,
                value: Int) : this(headerType, HttpResponseHeaderValue.valueOf(value))

    constructor(headerType: HttpResponseHeader,
                value: String) : this(headerType, HttpResponseHeaderValue.valueOf(value))

    public fun writeHeader(buffer: ByteBuffer) = value.writeHeader(type, buffer)
}
