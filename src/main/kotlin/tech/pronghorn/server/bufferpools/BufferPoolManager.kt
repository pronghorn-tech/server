package tech.pronghorn.server.bufferpools

import mu.KotlinLogging
import tech.pronghorn.plugins.spscQueue.SpscQueuePlugin
import java.nio.ByteBuffer

abstract class BufferPoolManager(private val bufferSize: Int,
                                 private val direct: Boolean = false) {
    private val logger = KotlinLogging.logger {}

    var threadsAccessed = mutableSetOf<Thread>()

    fun getBuffer(): PooledByteBuffer = pool.poll() ?: PooledByteBuffer(
            this,
            if(direct) ByteBuffer.allocateDirect(bufferSize) else ByteBuffer.allocate(bufferSize)
    )

    fun DEBUGgetBuffer(): PooledByteBuffer {
        threadsAccessed.add(Thread.currentThread())
        if(threadsAccessed.size > 1){
            try {
                throw Exception("Potato")
            }
            catch(ex: Exception){
                ex.printStackTrace()
            }
            logger.error("BufferPoolManager accessed from multiple threads! $this (${threadsAccessed.map { thread -> thread.name }.joinToString()})")
            System.exit(1)
        }
        val buffer = pool.poll() ?: PooledByteBuffer(this, ByteBuffer.allocate(bufferSize))
        if(buffer.buffer.position() != 0 || buffer.buffer.limit() != buffer.buffer.capacity()){
            logger.error("INVALID RECLAIMED BUFFER STATE")
            System.exit(1)
        }
        return buffer
    }

    fun release(buffer: PooledByteBuffer) = pool.offer(buffer)

    fun DEBUGrelease(buffer: PooledByteBuffer) {
        threadsAccessed.add(Thread.currentThread())
        if(threadsAccessed.size > 1){
            try {
                throw Exception("Potato")
            }
            catch(ex: Exception){
                ex.printStackTrace()
            }
            logger.error("BufferPoolManager accessed from multiple threads! $this (${threadsAccessed.map { thread -> thread.name }.joinToString()})")
            System.exit(1)
        }
        if(buffer.buffer.position() != 0 || buffer.buffer.limit() != buffer.buffer.capacity()){
            logger.error("INVALID RELEASED BUFFER STATE")
            System.exit(1)
        }
        pool.offer(buffer)
    }

    private val pool = SpscQueuePlugin.get<PooledByteBuffer>(1024)
}
