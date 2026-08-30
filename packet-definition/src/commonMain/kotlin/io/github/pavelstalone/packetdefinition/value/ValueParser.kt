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

package io.github.pavelstalone.packetdefinition.value

import java.nio.ByteBuffer
import kotlin.math.pow

/**
 * Interface for parsing data into a specific type
 *
 * @param T The type of the parsed value
 */
fun interface ValueParser<T> {

    /**
     * Parses the data from the [byteBuffer] into type [T]
     *
     * @param byteBuffer The data source
     * @return The parsed value
     */
    fun parse(byteBuffer: ByteBuffer): T

    /**
     * Parser for Boolean values
     */
    object BooleanParser : ValueParser<Boolean> {

        override fun parse(byteBuffer: ByteBuffer): Boolean {
            require(byteBuffer.hasRemaining()) { "Cannot parse empty byte array to Boolean" }

            while (byteBuffer.hasRemaining()) {
                if (byteBuffer.get() != 0.toByte()) return true
            }
            return false
        }
    }

    /**
     * Parser for Int values
     */
    object IntParser : ValueParser<Int> {

        override fun parse(byteBuffer: ByteBuffer): Int {
            require(byteBuffer.remaining() <= Int.SIZE_BYTES) { "Byte array too large for Int conversion: ${byteBuffer.remaining()} bytes" }

            if (byteBuffer.remaining() == Int.SIZE_BYTES) return byteBuffer.getInt()
            return parseUniversalInt(byteBuffer)
        }

        private fun parseUniversalInt(byteBuffer: ByteBuffer): Int {
            var num = byteBuffer.get().toInt()
            while (byteBuffer.hasRemaining()) {
                num = (num shl 8) or byteBuffer.get().toUByte().toInt()
            }

            return num
        }
    }

    /**
     * Parser for Float values. Supports standard Float (4 bytes) and SFLOAT (2 bytes).
     *
     * This implementation is part of the Packet Definition project, licensed under Apache 2.0.
     *
     * The SFLOAT parsing logic is adapted from the Nordic Semiconductor
     * Android BLE Library (no.nordicsemi.android.ble.data.Data),
     * which is licensed under the BSD 3-Clause License:
     *
     * Copyright (c) 2018, Nordic Semiconductor
     * All rights reserved.
     *
     * Redistribution and use in source and binary forms, with or without modification,
     * are permitted provided that the following conditions are met:
     *
     * 1. Redistributions of source code must retain the above copyright notice, this
     *    list of conditions and the following disclaimer.
     *
     * 2. Redistributions in binary form must reproduce the above copyright notice,
     *    this list of conditions and the following disclaimer in the documentation
     *    and/or other materials provided with the distribution.
     *
     * 3. Neither the name of the copyright holder nor the names of its contributors
     *    may be used to endorse or promote products derived from this software
     *    without specific prior written permission.
     *
     * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
     * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
     * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
     * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
     * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
     * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
     * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
     * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
     * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
     * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
     * POSSIBILITY OF SUCH DAMAGE.
     */
    object FloatParser : ValueParser<Float> {

        override fun parse(byteBuffer: ByteBuffer): Float {
            val byteCount = byteBuffer.remaining()
            require(byteCount == Short.SIZE_BYTES || byteCount == Float.SIZE_BYTES) {
                "Required size for float value is 4 bytes (IEEE 754) or 2 bytes (SFLOAT), got $byteCount"
            }

            if (byteCount == Float.SIZE_BYTES) return byteBuffer.float
            return parseSFloat(byteBuffer)
        }

        private fun parseSFloat(byteBuffer: ByteBuffer): Float {
            val b0 = byteBuffer.get()
            val b1 = byteBuffer.get()

            if (b1 == 0x07.toByte() && b0 == 0xFE.toByte()) return Float.POSITIVE_INFINITY
            if (b1 == 0x08.toByte() && b0 == 0x02.toByte()) return Float.NEGATIVE_INFINITY
            if ((b1 == 0x07.toByte() && b0 == 0xFF.toByte()) ||
                (b1 == 0x08.toByte() && b0 == 0x00.toByte()) ||
                (b1 == 0x08.toByte() && b0 == 0x01.toByte())
            ) return Float.NaN
            return bytesToFloat(b0, b1)
        }

        private fun bytesToFloat(b0: Byte, b1: Byte): Float {
            val mantissa = unsignedToSigned(
                unsignedByteToInt(b0) + ((unsignedByteToInt(b1) and 0x0F) shl 8),
                12
            )
            val exponent = unsignedToSigned(unsignedByteToInt(b1) ushr 4, 4)
            return (mantissa * 10f.pow(exponent.toFloat()))
        }

        private fun unsignedToSigned(unsigned: Int, size: Int): Int {
            var result = unsigned
            if ((unsigned and (1 shl size - 1)) != 0) {
                result = -1 * ((1 shl size - 1) - (unsigned and ((1 shl size - 1) - 1)))
            }
            return result
        }

        private fun unsignedByteToInt(byte: Byte): Int = byte.toUInt().toInt()
    }
}
