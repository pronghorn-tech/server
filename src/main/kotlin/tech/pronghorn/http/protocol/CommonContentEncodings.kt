package tech.pronghorn.http.protocol

import tech.pronghorn.http.HttpResponseHeaderValue
import tech.pronghorn.util.finder.ByteBacked

public interface ContentEncoding : ByteBacked {
    public fun getName(): String

    public fun asHeaderValue(): HttpResponseHeaderValue<*>
}

public class InstanceContentEncoding(private val value: ByteArray) : ContentEncoding {
    constructor(name: String) : this(name.toByteArray(Charsets.US_ASCII))

    override val bytes = ByteArray(0)

    override fun getName(): String = value.toString()

    override fun asHeaderValue(): HttpResponseHeaderValue<*> = HttpResponseHeaderValue.valueOf(value)
}


public enum class CommonContentEncodings(private val displayName: String) : ContentEncoding {
    Gzip("gzip"),
    Compress("compress"),
    Deflate("deflate"),
    Identity("identity"),
    Br("br");

    override val bytes: ByteArray = displayName.toByteArray(Charsets.US_ASCII)
    private val headerValue = HttpResponseHeaderValue.valueOf(bytes)

    override fun getName(): String = displayName

    override fun asHeaderValue(): HttpResponseHeaderValue<*> = headerValue
}
