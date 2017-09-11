package tech.pronghorn.util.finder

import tech.pronghorn.plugins.arrayHash.ArrayHasherPlugin
import java.nio.ByteBuffer
import java.util.*

internal class HashFinder<T : ByteBacked>(private val toLookup: Array<T>): ByteBackedFinder<T> {
    private val hashMap = HashMap<Long, T>()
    private val hasher = ArrayHasherPlugin.get()

    init {
        toLookup.forEach { value ->
            hashMap.put(hasher(value.bytes), value)
        }
    }

    override fun find(buffer: ByteBuffer, offset: Int, size: Int): T? {
        val bytes = ByteArray(size)
        val prePosition = buffer.position()
        buffer.position(offset)
        buffer.get(bytes)
        buffer.position(prePosition)
        return find(bytes)
    }

    override fun find(bytes: ByteArray): T? = hashMap.get(hasher(bytes))
}
