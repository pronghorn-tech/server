package tech.pronghorn.server.bufferpools

import tech.pronghorn.plugins.internalQueue.InternalQueuePlugin
import java.nio.ByteBuffer

class ReusableBufferPoolManager(private val bufferSize: Int,
                                private val direct: Boolean = false) {
    fun getBuffer(): ReusableByteBuffer = pool.poll() ?: ReusableByteBuffer(
            this,
            if (direct) ByteBuffer.allocateDirect(bufferSize) else ByteBuffer.allocate(bufferSize)
    )

    fun release(buffer: ReusableByteBuffer) {
        pool.offer(buffer)
    }

    private val pool = InternalQueuePlugin.get<ReusableByteBuffer>(1024)
}

