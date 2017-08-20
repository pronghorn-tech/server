package tech.pronghorn.util.finder

import java.nio.ByteBuffer

interface ByteBackedFinder<out T> {
    fun find(buffer: ByteBuffer,
             offset: Int,
             size: Int): T?

    fun find(bytes: ByteArray): T?
}
