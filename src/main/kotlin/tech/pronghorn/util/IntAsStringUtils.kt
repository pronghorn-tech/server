package tech.pronghorn.util

import java.nio.ByteBuffer

public fun stringLengthOfInt(value: Int): Int {
    return when {
        value < 10 -> 1
        value < 100 -> 2
        value < 1000 -> 3
        value < 10000 -> 4
        value < 100000 -> 5
        value < 1000000 -> 6
        value < 10000000 -> 7
        value < 100000000 -> 8
        value < 1000000000 -> 9
        else -> 10
    }
}

public fun stringLengthOfLong(value: Long): Int {
    return when {
        value < 10L -> 1
        value < 100L -> 2
        value < 1000L -> 3
        value < 10000L -> 4
        value < 100000L -> 5
        value < 1000000L -> 6
        value < 10000000L -> 7
        value < 100000000L -> 8
        value < 1000000000L -> 9
        value < 10000000000L -> 10
        value < 100000000000L -> 11
        value < 1000000000000L -> 12
        value < 10000000000000L -> 13
        value < 100000000000000L -> 14
        value < 1000000000000000L -> 15
        value < 10000000000000000L -> 16
        value < 100000000000000000L -> 17
        else -> 18
    }
}

public fun writeLongAsStringToBuffer(value: Long,
                              buffer: ByteBuffer) {
    if (value < Int.MAX_VALUE) {
        writeIntAsStringToBuffer(value.toInt(), buffer)
    }
    else {
        if (value >= 100000000000000000L) buffer.put((48 + (value.rem(1000000000000000000L) / 100000000000000000L)).toByte())
        if (value >= 10000000000000000L) buffer.put((48 + (value.rem(100000000000000000L) / 10000000000000000L)).toByte())
        if (value >= 1000000000000000L) buffer.put((48 + (value.rem(10000000000000000L) / 1000000000000000L)).toByte())
        if (value >= 100000000000000L) buffer.put((48 + (value.rem(1000000000000000L) / 100000000000000L)).toByte())
        if (value >= 10000000000000L) buffer.put((48 + (value.rem(100000000000000L) / 10000000000000L)).toByte())
        if (value >= 1000000000000L) buffer.put((48 + (value.rem(10000000000000L) / 1000000000000L)).toByte())
        if (value >= 100000000000L) buffer.put((48 + (value.rem(1000000000000L) / 100000000000L)).toByte())
        if (value >= 10000000000L) buffer.put((48 + (value.rem(100000000000L) / 10000000000L)).toByte())
        if (value >= 1000000000L) buffer.put((48 + (value.rem(10000000000L) / 1000000000L)).toByte())
        if (value >= 100000000L) buffer.put((48 + (value.rem(1000000000L) / 100000000L)).toByte())
        if (value >= 10000000L) buffer.put((48 + (value.rem(100000000L) / 10000000L)).toByte())
        if (value >= 1000000L) buffer.put((48 + (value.rem(10000000L) / 1000000L)).toByte())
        if (value >= 100000L) buffer.put((48 + (value.rem(1000000L) / 100000L)).toByte())
        if (value >= 10000L) buffer.put((48 + (value.rem(100000L) / 10000L)).toByte())
        if (value >= 1000L) buffer.put((48 + (value.rem(10000L) / 1000L)).toByte())
        if (value >= 100L) buffer.put((48 + (value.rem(1000L) / 100L)).toByte())
        if (value >= 10L) buffer.put((48 + (value.rem(100L) / 10L)).toByte())
        buffer.put((48 + value.rem(10)).toByte())
    }
}

public fun toHex(value: Int): Byte {
    val hex = value.rem(16)
    if (hex < 10) {
        return (48 + hex).toByte()
    }
    else {
        return (55 + hex).toByte()
    }
}

public fun toHex(value: Long): Byte {
    val hex = value.rem(16)
    if (hex < 10) {
        return (48 + hex).toByte()
    }
    else {
        return (55 + hex).toByte()
    }
}

public fun stringLengthAsHexOfInt(value: Int): Int {
    return when {
        value < 16 -> 1
        value < 256 -> 2
        value < 4096 -> 3
        value < 65536 -> 4
        value < 1048576 -> 5
        value < 16777216 -> 6
        value < 268435456 -> 7
        else -> 8
    }
}

public fun stringLengthAsHexOfLong(value: Long): Int {
    return when {
        value < 16 -> 1
        value < 256 -> 2
        value < 4096 -> 3
        value < 65536 -> 4
        value < 1048576 -> 5
        value < 16777216 -> 6
        value < 268435456 -> 7
        value < 4294967296 -> 8
        value < 68719476736 -> 9
        value < 1099511627776 -> 10
        value < 17592186044416 -> 11
        value < 281474976710656 -> 12
        value < 4503599627370496 -> 13
        value < 72057594037927936 -> 14
        else -> 15
    }
}

public fun writeLongAsHexToByteArray(value: Long,
                              array: ByteArray) {
    if (value < Int.MAX_VALUE) {
        writeIntAsHexToByteArray(value.toInt(), array)
    }
    else {
        var x = 0
        if (value >= 72057594037927936) array[x++] = toHex(value.rem(1152921504606846976) / 72057594037927936)
        if (value >= 4503599627370496) array[x++] = toHex(value.rem(72057594037927936) / 4503599627370496)
        if (value >= 281474976710656) array[x++] = toHex(value.rem(4503599627370496) / 281474976710656)
        if (value >= 17592186044416) array[x++] = toHex(value.rem(281474976710656) / 17592186044416)
        if (value >= 1099511627776) array[x++] = toHex(value.rem(17592186044416) / 1099511627776)
        if (value >= 68719476736) array[x++] = toHex(value.rem(1099511627776) / 68719476736)
        if (value >= 4294967296) array[x++] = toHex(value.rem(68719476736) / 4294967296)
        if (value >= 268435456) array[x++] = toHex(value.rem(4294967296) / 268435456)
        if (value >= 16777216) array[x++] = toHex(value.rem(268435456) / 16777216)
        if (value >= 1048576) array[x++] = toHex(value.rem(16777216) / 1048576)
        if (value >= 65536) array[x++] = toHex(value.rem(1048576) / 65536)
        if (value >= 4096) array[x++] = toHex(value.rem(65536) / 4096)
        if (value >= 256) array[x++] = toHex(value.rem(4096) / 256)
        if (value >= 16) array[x++] = toHex(value.rem(256) / 16)
        array[x] = toHex(value.rem(16))
    }
}

public fun writeLongAsHexToBuffer(value: Long,
                               buffer: ByteBuffer) {
    if (value < Int.MAX_VALUE) {
        writeIntAsHexToBuffer(value.toInt(), buffer)
    }
    else {
        if (value >= 72057594037927936) buffer.put(toHex(value.rem(1152921504606846976) / 72057594037927936))
        if (value >= 4503599627370496) buffer.put(toHex(value.rem(72057594037927936) / 4503599627370496))
        if (value >= 281474976710656) buffer.put(toHex(value.rem(4503599627370496) / 281474976710656))
        if (value >= 17592186044416) buffer.put(toHex(value.rem(281474976710656) / 17592186044416))
        if (value >= 1099511627776) buffer.put(toHex(value.rem(17592186044416) / 1099511627776))
        if (value >= 68719476736) buffer.put(toHex(value.rem(1099511627776) / 68719476736))
        if (value >= 4294967296) buffer.put(toHex(value.rem(68719476736) / 4294967296))
        if (value >= 268435456) buffer.put(toHex(value.rem(4294967296) / 268435456))
        if (value >= 16777216) buffer.put(toHex(value.rem(268435456) / 16777216))
        if (value >= 1048576) buffer.put(toHex(value.rem(16777216) / 1048576))
        if (value >= 65536) buffer.put(toHex(value.rem(1048576) / 65536))
        if (value >= 4096) buffer.put(toHex(value.rem(65536) / 4096))
        if (value >= 256) buffer.put(toHex(value.rem(4096) / 256))
        if (value >= 16) buffer.put(toHex(value.rem(256) / 16))
        buffer.put(toHex(value.rem(16)))
    }
}

public fun writeIntAsHexToBuffer(value: Int,
                           buffer: ByteBuffer) {
    if (value >= 268435456) buffer.put(toHex(value.toLong().rem(4294967296L).toInt() / 268435456))
    if (value >= 16777216) buffer.put(toHex(value.rem(268435456) / 16777216))
    if (value >= 1048576) buffer.put(toHex(value.rem(16777216) / 1048576))
    if (value >= 65536) buffer.put(toHex(value.rem(1048576) / 65536))
    if (value >= 4096) buffer.put(toHex(value.rem(65536) / 4096))
    if (value >= 256) buffer.put(toHex(value.rem(4096) / 256))
    if (value >= 16) buffer.put(toHex(value.rem(256) / 16))
    buffer.put(toHex(value.rem(16)))
}

public fun writeIntAsHexToByteArray(value: Int,
                             array: ByteArray) {
    var x = 0
    if (value >= 268435456) array[x++] = toHex(value.toLong().rem(4294967296L).toInt() / 268435456)
    if (value >= 16777216) array[x++] = toHex(value.rem(268435456) / 16777216)
    if (value >= 1048576) array[x++] = toHex(value.rem(16777216) / 1048576)
    if (value >= 65536) array[x++] = toHex(value.rem(1048576) / 65536)
    if (value >= 4096) array[x++] = toHex(value.rem(65536) / 4096)
    if (value >= 256) array[x++] = toHex(value.rem(4096) / 256)
    if (value >= 16) array[x++] = toHex(value.rem(256) / 16)
    array[x] = toHex(value.rem(16))
}

public fun writeLongAsStringToByteArray(value: Long,
                                 array: ByteArray) {
    if (value < Int.MAX_VALUE) {
        writeIntAsStringToByteArray(value.toInt(), array)
    }
    else {
        var x = 0
        if (value >= 100000000000000000L) array[x++] = ((48 + (value.rem(1000000000000000000L) / 100000000000000000L)).toByte())
        if (value >= 10000000000000000L) array[x++] = ((48 + (value.rem(100000000000000000L) / 10000000000000000L)).toByte())
        if (value >= 1000000000000000L) array[x++] = ((48 + (value.rem(10000000000000000L) / 1000000000000000L)).toByte())
        if (value >= 100000000000000L) array[x++] = ((48 + (value.rem(1000000000000000L) / 100000000000000L)).toByte())
        if (value >= 10000000000000L) array[x++] = ((48 + (value.rem(100000000000000L) / 10000000000000L)).toByte())
        if (value >= 1000000000000L) array[x++] = ((48 + (value.rem(10000000000000L) / 1000000000000L)).toByte())
        if (value >= 100000000000L) array[x++] = ((48 + (value.rem(1000000000000L) / 100000000000L)).toByte())
        if (value >= 10000000000L) array[x++] = ((48 + (value.rem(100000000000L) / 10000000000L)).toByte())
        if (value >= 1000000000L) array[x++] = ((48 + (value.rem(10000000000L) / 1000000000L)).toByte())
        if (value >= 100000000L) array[x++] = ((48 + (value.rem(1000000000L) / 100000000L)).toByte())
        if (value >= 10000000L) array[x++] = ((48 + (value.rem(100000000L) / 10000000L)).toByte())
        if (value >= 1000000L) array[x++] = ((48 + (value.rem(10000000L) / 1000000L)).toByte())
        if (value >= 100000L) array[x++] = ((48 + (value.rem(1000000L) / 100000L)).toByte())
        if (value >= 10000L) array[x++] = ((48 + (value.rem(100000L) / 10000L)).toByte())
        if (value >= 1000L) array[x++] = ((48 + (value.rem(10000L) / 1000L)).toByte())
        if (value >= 100L) array[x++] = ((48 + (value.rem(1000L) / 100L)).toByte())
        if (value >= 10L) array[x++] = ((48 + (value.rem(100L) / 10L)).toByte())
        array[x] = ((48 + value.rem(10)).toByte())
    }
}

public fun writeIntAsStringToBuffer(value: Int,
                             buffer: ByteBuffer) {
    if (value >= 1000000000) buffer.put((48 + (value.rem(10000000000) / 1000000000)).toByte())
    if (value >= 100000000) buffer.put((48 + (value.rem(1000000000) / 100000000)).toByte())
    if (value >= 10000000) buffer.put((48 + (value.rem(100000000) / 10000000)).toByte())
    if (value >= 1000000) buffer.put((48 + (value.rem(10000000) / 1000000)).toByte())
    if (value >= 100000) buffer.put((48 + (value.rem(1000000) / 100000)).toByte())
    if (value >= 10000) buffer.put((48 + (value.rem(100000) / 10000)).toByte())
    if (value >= 1000) buffer.put((48 + (value.rem(10000) / 1000)).toByte())
    if (value >= 100) buffer.put((48 + (value.rem(1000) / 100)).toByte())
    if (value >= 10) buffer.put((48 + (value.rem(100) / 10)).toByte())
    buffer.put((48 + value.rem(10)).toByte())
}

public fun writeIntAsStringToByteArray(value: Int,
                                array: ByteArray) {
    var x = 0
    if (value >= 1000000000) array[x++] = ((48 + (value.rem(10000000000) / 1000000000)).toByte())
    if (value >= 100000000) array[x++] = ((48 + (value.rem(1000000000) / 100000000)).toByte())
    if (value >= 10000000) array[x++] = ((48 + (value.rem(100000000) / 10000000)).toByte())
    if (value >= 1000000) array[x++] = ((48 + (value.rem(10000000) / 1000000)).toByte())
    if (value >= 100000) array[x++] = ((48 + (value.rem(1000000) / 100000)).toByte())
    if (value >= 10000) array[x++] = ((48 + (value.rem(100000) / 10000)).toByte())
    if (value >= 1000) array[x++] = ((48 + (value.rem(10000) / 1000)).toByte())
    if (value >= 100) array[x++] = ((48 + (value.rem(1000) / 100)).toByte())
    if (value >= 10) array[x++] = ((48 + (value.rem(100) / 10)).toByte())
    array[x] = ((48 + value.rem(10)).toByte())
}
