package tech.pronghorn.server.bufferpools

import java.nio.ByteBuffer

class PooledByteBuffer(private val manager: BufferPoolManager,
                       val buffer: ByteBuffer) {
    fun release() {
        buffer.clear()
        manager.release(this)
    }

//    protected fun finalize() {
//        if(buffer.position() != 0){
//            println("PooledByteBuffer not cleared!")
//            System.exit(1)
//        }
//    }
}
