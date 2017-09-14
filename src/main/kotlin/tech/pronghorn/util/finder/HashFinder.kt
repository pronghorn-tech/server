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
import java.util.HashMap

internal class HashFinder<T : ByteBacked>(private val toLookup: Array<T>) : ByteBackedFinder<T> {
    private val hashMap = HashMap<Long, T>()
    private val hasher = ArrayHasherPlugin.get()

    init {
        toLookup.forEach { value ->
            hashMap.put(hasher(value.bytes), value)
        }
    }

    override fun find(buffer: ByteBuffer, offset: Int, size: Int): T? {
        val bytes = ByteArray(size)
        val prePosition = buffer.position()
        buffer.position(offset)
        buffer.get(bytes)
        buffer.position(prePosition)
        return find(bytes)
    }

    override fun find(bytes: ByteArray): T? = hashMap[hasher(bytes)]
}
