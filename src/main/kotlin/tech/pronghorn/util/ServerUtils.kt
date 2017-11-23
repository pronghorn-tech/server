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

package tech.pronghorn.util

import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

public fun SocketChannel.write(string: String) {
    val byteArray = string.toByteArray(StandardCharsets.UTF_8)
    if (byteArray.size > 4096) {
        throw Exception("SocketChannel.write(String) is strictly for short strings.")
    }
    val buffer = ByteBuffer.wrap(byteArray)
    assert(write(buffer) == byteArray.size)
}

public fun ByteBuffer.sliceToArray(start: Int,
                            length: Int): ByteArray {
    val slice = ByteArray(length)
    val prePosition = position()
    if (prePosition != start) {
        position(start)
    }
    get(slice)
    position(prePosition)
    return slice
}
