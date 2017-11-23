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

import tech.pronghorn.plugins.arrayHash.ArrayHasherPlugin
import java.nio.ByteBuffer
import java.util.Arrays
import java.util.HashMap

internal const val mostDifferentiatedCutoff = 4

internal class DifferentiatedFinder<T : ByteBacked>(toLookup: Array<T>) : ByteBackedFinder<T> {
    private val maxLength = toLookup.map(ByteBacked::bytes).map { b -> b.size }.max() ?: 0
    private val byLength = arrayOfNulls<Array<ByteBacked>>(maxLength)
    private val mostDifferentiatedBytes = Array(maxLength, { Differentiation() })
    private val hashMap = HashMap<Long, T>(toLookup.size)
    private val hasher = ArrayHasherPlugin.get()

    private class Differentiation(val byteIndex: Int = 0,
                          val duplicateCount: Int = 0)

    init {
        toLookup.forEach { value ->
            hashMap.put(hasher(value.bytes), value)
        }

        var x = 1
        while (x < maxLength) {
            val atThisLength = toLookup.filter { backed -> backed.bytes.size == x + 1 }.toTypedArray<ByteBacked>()
            if (atThisLength.isNotEmpty()) {
                var mostDifferentiatedByte = 0
                var mostDuplicateCount = Int.MAX_VALUE
                val counts = IntArray(256)
                var byteIndex = 0

                while (byteIndex < x) {
                    var objIndex = 0
                    while (objIndex < atThisLength.size) {
                        counts[atThisLength[objIndex].bytes[byteIndex].toInt()] += 1
                        objIndex += 1
                    }

                    val mostCount = counts.max() ?: 0
                    if (mostCount < mostDuplicateCount) {
                        mostDifferentiatedByte = byteIndex
                        mostDuplicateCount = mostCount
                    }

                    var y = 0
                    while (y < counts.size) {
                        counts[y] = 0
                        y += 1
                    }

                    byteIndex += 1
                }

                mostDifferentiatedBytes[x] = Differentiation(mostDifferentiatedByte, mostDuplicateCount)
                byLength[x] = atThisLength
            }
            x += 1
        }
    }

    private fun findByHash(buffer: ByteBuffer, offset: Int, size: Int): T? {
        val bytes = ByteArray(size)
        val prePosition = buffer.position()
        buffer.position(offset)
        buffer.get(bytes)
        buffer.position(prePosition)
        return find(bytes)
    }

    private fun findByHash(bytes: ByteArray): T? = hashMap[hasher(bytes)]

    override fun find(buffer: ByteBuffer,
                      offset: Int,
                      size: Int): T? {
        if (size > maxLength) {
            return null
        }

        val index = size - 1
        val allPossible = byLength[index] ?: return null

        if (allPossible.size < mostDifferentiatedCutoff) {
            @Suppress("UNCHECKED_CAST")
            return allPossible.find { possible -> isEqual(possible.bytes, buffer, offset, size) } as T?
        }
        else {
            val mostDifferentiated = mostDifferentiatedBytes[index]
            if (mostDifferentiated.duplicateCount > mostDifferentiatedCutoff) {
                return findByHash(buffer, offset, size)
            }
            else if (mostDifferentiated.byteIndex > 0) {
                @Suppress("UNCHECKED_CAST")
                return allPossible.find { possible -> isEqualStartingAt(possible.bytes, buffer, offset, size, mostDifferentiated.byteIndex) } as T?
            }
            else {
                @Suppress("UNCHECKED_CAST")
                return allPossible.find { possible -> isEqual(possible.bytes, buffer, offset, size) } as T?
            }
        }
    }

    override fun find(bytes: ByteArray): T? {
        if (bytes.size > maxLength) {
            return null
        }

        val index = bytes.size - 1
        val allPossible = byLength[index] ?: return null

        if (allPossible.size < mostDifferentiatedCutoff) {
            @Suppress("UNCHECKED_CAST")
            return allPossible.find { possible -> Arrays.equals(possible.bytes, bytes) } as T?
        }
        else {
            val mostDifferentiated = mostDifferentiatedBytes[index]
            if (mostDifferentiated.duplicateCount > mostDifferentiatedCutoff) {
                return findByHash(bytes)
            }
            else if (mostDifferentiated.byteIndex > 0) {
                @Suppress("UNCHECKED_CAST")
                return allPossible.find { possible -> isEqualStartingAt(possible.bytes, bytes, mostDifferentiated.byteIndex) } as T?
            }
            else {
                @Suppress("UNCHECKED_CAST")
                return allPossible.find { possible -> Arrays.equals(possible.bytes, bytes) } as T?
            }
        }
    }
}
