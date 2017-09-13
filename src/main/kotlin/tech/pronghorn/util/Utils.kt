package tech.pronghorn.util

import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

fun SocketChannel.write(string: String) {
    val byteArray = string.toByteArray(StandardCharsets.UTF_8)
    if (byteArray.size > 4096) {
        throw Exception("SocketChannel.write(String) is strictly for short strings.")
    }
    val buffer = ByteBuffer.wrap(byteArray)
    assert(write(buffer) == byteArray.size)
}

fun ByteBuffer.sliceToArray(start: Int,
                            length: Int): ByteArray {
    val slice = ByteArray(length)
    val prePosition = position()
    if (prePosition != start) {
        position(start)
    }
    get(slice)
    position(prePosition)
    return slice
}

fun runAllIgnoringExceptions(vararg blocks: () -> Unit) {
    blocks.forEach { block ->
        try {
            block()
        }
        catch (ex: Exception) {
            // no-op
        }
    }
}
