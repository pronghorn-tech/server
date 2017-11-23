package tech.pronghorn.http.protocol

import java.util.Arrays
import java.util.Objects

private val trueBytes = "true".toByteArray(Charsets.US_ASCII)
private val falseBytes = "false".toByteArray(Charsets.US_ASCII)

public class QueryParam(public val name: ByteArray,
                        public val value: ByteArray) {
    constructor(name: String,
                value: String) : this(name.toByteArray(Charsets.US_ASCII), value.toByteArray(Charsets.US_ASCII))

    public fun valueAsBoolean(): Boolean? {
        if (value.size == 1) {
            when (value[0]) {
                zeroByte -> return false
                oneByte -> return true
                else -> return null
            }
        }
        else if (Arrays.equals(value, trueBytes)) {
            return true
        }
        else if (Arrays.equals(value, falseBytes)) {
            return false
        }
        else {
            return null
        }
    }

    public fun valueAsInt(): Int? {
        var intValue = 0
        var x = 0
        while (x < value.size) {
            val byte = value[x]
            if (byte < 48 || byte > 57) {
                return null
            }
            intValue *= 10
            intValue += byte - 48
            x += 1
        }

        return intValue
    }

    public fun valueAsLong(): Long? {
        var longValue = 0L
        var x = 0
        while (x < value.size) {
            val byte = value[x]
            if (byte < 48 || byte > 57) {
                return null
            }
            longValue *= 10
            longValue += byte - 48
            x += 1
        }

        return longValue
    }

    public fun valueAsString(): String = String(value, Charsets.US_ASCII)

    override fun equals(other: Any?): Boolean {
        return when (other) {
            this === other -> true
            is QueryParam -> {
                return Arrays.equals(name, other.name) && Arrays.equals(value, other.name)
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(name, value)
    }
}
