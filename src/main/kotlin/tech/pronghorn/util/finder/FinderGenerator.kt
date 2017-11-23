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

internal object FinderGenerator {
    internal fun <T : ByteBacked> generateFinder(toLookup: Array<T>): ByteBackedFinder<T> {
        val byBytes = toLookup.map(ByteBacked::bytes)
        val groupedBySize = byBytes.groupBy { bytes -> bytes.size }
        val maxOfOneLength = groupedBySize.values.map { list -> list.size }.max() ?: 0

        return when {
            byBytes.size < 4 -> SequentialFinder(toLookup)
            maxOfOneLength < 5 -> LengthPartitionedFinder(toLookup)
            else -> DifferentiatedFinder(toLookup)
        }
    }
}
