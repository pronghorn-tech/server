package tech.pronghorn.server.bufferpools

import java.nio.ByteBuffer

sealed class ManagedByteBuffer {
    abstract val buffer: ByteBuffer
    abstract fun release()
}

class ReusableByteBuffer(private val manager: ReusableBufferPoolManager,
                         override val buffer: ByteBuffer) : ManagedByteBuffer() {
    override fun release() {
        buffer.clear()
        manager.release(this)
    }
}

class OneUseByteBuffer(bufferSize: Int,
                       direct: Boolean) : ManagedByteBuffer() {
    override val buffer: ByteBuffer = if (direct) ByteBuffer.allocateDirect(bufferSize) else ByteBuffer.allocate(bufferSize)

    override fun release() {
        buffer.clear()
    }
}
