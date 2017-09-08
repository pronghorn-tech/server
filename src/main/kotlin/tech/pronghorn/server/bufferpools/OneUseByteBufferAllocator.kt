package tech.pronghorn.server.bufferpools

class OneUseByteBufferAllocator(private val direct: Boolean = false) {
    fun getBuffer(bufferSize: Int): OneUseByteBuffer = OneUseByteBuffer(bufferSize, direct)
}
