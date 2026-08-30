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

package io.github.pavelstalone.packetdefinition.validation

import io.github.pavelstalone.core.BitBuffer
import io.github.pavelstalone.core.DataSize.Companion.bytes
import io.github.pavelstalone.core.calculateByteSize
import io.github.pavelstalone.core.forEach
import io.github.pavelstalone.core.sumOf
import io.github.pavelstalone.packetdefinition.value.Value
import java.nio.ByteBuffer

/**
 * CRC-32 implementation of packet validation
 *
 * The algorithm operates on all bytes accumulated from the provided filled values,
 * in the exact order and bit arrangement as specified by the values
 *
 * @property crc the expected CRC-32 value that a valid packet should produce
 */
class CRC32Validation(
    private val crc: Int
) : PacketValidation {

    override fun validate(vararg values: Value<*>): Boolean {
        return calculate(values = values) == crc
    }

    companion object {

        private val crcTable
            get() = LongArray(256) { i ->
                var j = i.toLong()

                repeat(8) {
                    j = if ((j and 1L) == 1L) {
                        (j ushr 1) xor 3988292384L
                    } else {
                        j ushr 1
                    }
                }

                j
            }

        /**
         * Calculates the CRC32 value for a list of [Value]s
         *
         * @param values The values to include in the calculation
         * @return The calculated CRC32 integer
         */
        fun calculate(vararg values: Value<*>): Int {
            val size = calculateByteSize(values.sumOf { it.size })
            val bitBuffer = BitBuffer.Companion.allocate(size.bytes).apply {
                values.forEach { filledValue ->
                    put(bytes = filledValue.bytes, dataSize = filledValue.size)
                }
            }

            bitBuffer.rewind()
            return calculate(bitBuffer.getAll())
        }

        /**
         * Calculates the CRC32 value for a [java.nio.ByteBuffer]
         *
         * @param bytes The data buffer
         * @return The calculated CRC32 integer
         */
        fun calculate(bytes: ByteBuffer): Int {
            val table = crcTable

            var calculatedCRC: Long = 0
            bytes.forEach { byte ->
                val idx = ((byte.toLong() xor calculatedCRC) and 255L).toInt()
                calculatedCRC = (calculatedCRC ushr 8) xor table[idx]
            }

            return calculatedCRC.toInt()
        }
    }
}