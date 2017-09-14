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

package tech.pronghorn.util.finder

import java.nio.ByteBuffer
import java.util.Arrays

internal class LengthPartitionedFinder<T : ByteBacked>(toLookup: Array<T>) : ByteBackedFinder<T> {
    private val maxLength = toLookup.map(ByteBacked::bytes).map { b -> b.size }.max() ?: 0
    private val byLength = arrayOfNulls<Array<ByteBacked>>(maxLength)

    init {
        var x = 1
        while (x < maxLength) {
            val list = toLookup.filter { backed -> backed.bytes.size == x + 1 }
            if (list.isNotEmpty()) {
                val arr = Array<ByteBacked>(list.size, { index -> list[index] })
                byLength[x] = arr
            }
            x += 1
        }
    }

    override fun find(buffer: ByteBuffer,
                      offset: Int,
                      size: Int): T? {
        if (size > maxLength) {
            return null
        }

        val allPossible = byLength[size - 1] ?: return null
        @Suppress("UNCHECKED_CAST")
        return allPossible.find { possible -> isEqual(possible.bytes, buffer, offset, size) } as T?
    }

    override fun find(bytes: ByteArray): T? {
        if (bytes.size > maxLength) {
            return null
        }
        val allPossible = byLength[bytes.size - 1] ?: return null
        @Suppress("UNCHECKED_CAST")
        return allPossible.find { possible -> Arrays.equals(possible.bytes, bytes) } as T?
    }
}
