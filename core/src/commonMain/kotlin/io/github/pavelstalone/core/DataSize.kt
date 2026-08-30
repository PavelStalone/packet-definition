/*
 * Copyright 2026 Pavel Shoplik
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

package io.github.pavelstalone.core

import io.github.pavelstalone.core.DataSize.Companion.bits
import kotlin.math.max

/**
 * Represents a data size in bits
 *
 * @property bitSize number of bits in the size
 */
@JvmInline
value class DataSize(val bitSize: Int) {

    init {
        require(bitSize >= 0) { "Size value cannot be negative" }
    }

    /**
     * Adds two sizes and returns a new [DataSize] object with the total number of bits
     *
     * @param dataSize size to add to the current one
     * @return New [DataSize] object containing the sum of bits
     */
    operator fun plus(dataSize: DataSize) = DataSize(bitSize + dataSize.bitSize)

    /**
     * Subtracts a size from the current one and returns a new [DataSize]
     *
     * This operation ensures the result is never negative by using a maximum of 0
     * for negative differences
     *
     * @param dataSize size to subtract from the current one
     * @return New [DataSize] object containing the difference (minimum value of 0)
     */
    operator fun minus(dataSize: DataSize) = DataSize(max(0, bitSize - dataSize.bitSize))

    override fun toString() = "$bitSize bits"

    companion object {

        /**
         * Converts an integer to a size in bytes
         *
         * @return [DataSize] representing the specified number of bytes in bits
         */
        val Int.bytes: DataSize
            get() = DataSize(this * 8)

        /**
         * Converts an integer to a size in bits
         *
         * @return [DataSize] representing the specified number of bits
         */
        val Int.bits: DataSize
            get() = DataSize(this)
    }
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the array
 *
 * @param selector function that maps each element to a [DataSize]
 * @return the total [DataSize]
 */
inline fun <T> Array<out T>.sumOf(selector: (T) -> DataSize): DataSize {
    var sum = 0.bits
    for (element in this) {
        sum += selector(element)
    }

    return sum
}
