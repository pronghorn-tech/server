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

internal class SequentialFinder<T : ByteBacked>(private val toLookup: Array<T>) : ByteBackedFinder<T> {
    private val maxLength = toLookup.map(ByteBacked::bytes).map { b -> b.size }.max() ?: 0

    override fun find(buffer: ByteBuffer, offset: Int, size: Int): T? {
        if (size > maxLength) {
            return null
        }

        var x = 0
        while (x < toLookup.size) {
            if (isEqual(toLookup[x].bytes, buffer, offset, size)) {
                return toLookup[x]
            }
            x += 1
        }
        return null
    }

    override fun find(bytes: ByteArray): T? {
        if (bytes.size > maxLength) {
            return null
        }

        var x = 0
        while (x < toLookup.size) {
            if (Arrays.equals(toLookup[x].bytes, bytes)) {
                return toLookup[x]
            }
            x += 1
        }
        return null
    }

}
