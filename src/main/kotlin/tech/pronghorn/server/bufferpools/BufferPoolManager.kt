package tech.pronghorn.server.bufferpools

import mu.KotlinLogging
import org.jctools.queues.SpscArrayQueue
import java.nio.ByteBuffer

abstract class BufferPoolManager(val bufferSize: Int) {
    private val logger = KotlinLogging.logger {}

    var threadsAccessed = mutableSetOf<Thread>()

    fun getBuffer(): PooledByteBuffer {
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

    fun release(buffer: PooledByteBuffer) {
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

    private val pool = SpscArrayQueue<PooledByteBuffer>(1024)
}
