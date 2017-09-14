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

internal fun isEqual(a1: ByteArray, a2: ByteArray, offset: Int, size: Int): Boolean {
    if (a2.size != size) {
        return false
    }

    var x = 0
    while (x < size) {
        if (a1[offset + x] != a2[x]) {
            return false
        }
        x += 1
    }
    return true
}

internal fun isEqualStartingAt(a1: ByteArray, a2: ByteArray, startingAt: Int): Boolean {
    if (a1.size != a2.size) {
        return false
    }

    var x = 0
    while (x < a1.size) {
        val index = (startingAt + x) % a1.size
        if (a1[index] != a2[index]) {
            return false
        }
        x += 1
    }
    return true
}

internal fun isEqualStartingAt(arr: ByteArray, buffer: ByteBuffer, offset: Int, size: Int, startingAt: Int): Boolean {
    val prePosition = buffer.position()
    if (arr.size != size) {
        return false
    }

    buffer.position(offset + startingAt)
    var x = startingAt
    while (x < size) {
        if (buffer.get() != arr[x]) {
            buffer.position(prePosition)
            return false
        }
        x += 1
    }

    x = 0
    buffer.position(offset)
    while (x < startingAt) {
        if (buffer.get() != arr[x]) {
            buffer.position(prePosition)
            return false
        }
        x += 1
    }

    buffer.position(prePosition)
    return true
}

internal fun isEqual(arr: ByteArray, buffer: ByteBuffer, offset: Int, size: Int): Boolean {
    val prePosition = buffer.position()
    if (arr.size != size) {
        return false
    }

    buffer.position(offset)
    var x = 0
    while (x < size) {
        if (buffer.get() != arr[x]) {
            buffer.position(prePosition)
            return false
        }
        x += 1
    }

    buffer.position(prePosition)
    return true
}
