/*
 * Copyright 2017 Pronghorn Technology LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
