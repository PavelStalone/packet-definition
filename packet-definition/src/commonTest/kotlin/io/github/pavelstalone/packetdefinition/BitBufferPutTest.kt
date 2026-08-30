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

package io.github.pavelstalone.packetdefinition

import io.github.pavelstalone.core.BitBuffer
import io.github.pavelstalone.core.DataSize.Companion.bits
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals

class BitBufferPutTest {

    @Test
    fun `put simple bit segments`() {
        val bitBuffer = BitBuffer.wrap(ByteArray(1))
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0b0111_0001)), 1.bits)
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0b1111_1101.toByte())), 2.bits)

        val result = bitBuffer.getAll()
        assertContentEquals(byteArrayOf(0b1010_0000.toByte()), result.array())
    }

    @Test
    fun `put bits spanning byte boundary`() {
        val bitBuffer = BitBuffer.wrap(ByteArray(2))
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0b0000_0000)), 5.bits)
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0b0011_0101)), 6.bits)

        val result = bitBuffer.getAll()
        assertContentEquals(byteArrayOf(0b0000_0110, 0b1010_0000.toByte()), result.array())
    }

    @Test
    fun `put multiple bit segments into single byte`() {
        val bitBuffer = BitBuffer.wrap(ByteArray(1))
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0b0000_0000)), 5.bits)
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0b0011_0101)), 2.bits)
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0b0011_0101)), 1.bits)

        val result = bitBuffer.getAll()
        assertContentEquals(byteArrayOf(0b0000_0011), result.array())
    }

    @Test
    fun `put aligned bytes`() {
        val bitBuffer = BitBuffer.wrap(ByteArray(2))
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0xAA.toByte(), 0xBB.toByte())), 16.bits)

        val result = bitBuffer.getAll()
        assertContentEquals(byteArrayOf(0xAA.toByte(), 0xBB.toByte()), result.array())
    }

    @Test
    fun `put zero bits`() {
        val bytes = byteArrayOf(0x00)
        val bitBuffer = BitBuffer.wrap(bytes)
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0xFF.toByte())), 0.bits)

        val result = bitBuffer.getAll()
        assertContentEquals(byteArrayOf(0x00), result.array())
    }

    @Test
    fun `put consecutive single bits`() {
        val bitBuffer = BitBuffer.wrap(ByteArray(1))
        repeat(8) { bitBuffer.put(ByteBuffer.wrap(byteArrayOf(1)), 1.bits) }

        val result = bitBuffer.getAll()
        assertContentEquals(byteArrayOf(0xFF.toByte()), result.array())
    }

    @Test
    fun `put byte with 4 bit offset`() {
        val bitBuffer = BitBuffer.wrap(ByteArray(2))
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0)), 4.bits)
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0xAA.toByte())), 8.bits)

        val result = bitBuffer.getAll()
        assertContentEquals(byteArrayOf(0x0A, 0xA0.toByte()), result.array())
    }

    @Test
    fun `put int with 4 bit offset`() {
        val bitBuffer = BitBuffer.wrap(ByteArray(5))
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0)), 4.bits)

        val intValue = 0x12345678
        val intBytes = ByteBuffer.allocate(4).putInt(intValue)
        bitBuffer.put(intBytes.rewind(), 32.bits)

        val result = bitBuffer.getAll()
        assertContentEquals(byteArrayOf(0x01, 0x23, 0x45, 0x67, 0x80.toByte()), result.array())
    }

    @Test
    fun `put long aligned 64 bits`() {
        val bitBuffer = BitBuffer.wrap(ByteArray(8))
        val longValue = -1L // All ones
        val longBytes = ByteBuffer.allocate(8).putLong(longValue).array()
        bitBuffer.put(ByteBuffer.wrap(longBytes), 64.bits)

        val result = bitBuffer.getAll()
        assertContentEquals(ByteArray(8) { 0xFF.toByte() }, result.array())
    }

    @Test
    fun `put long with 4 bit offset`() {
        val bitBuffer = BitBuffer.wrap(ByteArray(9))
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0)), 4.bits)

        val longValue = 0x1234567890ABCDEFL
        val longBytes = ByteBuffer.allocate(8).putLong(longValue).array()
        bitBuffer.put(ByteBuffer.wrap(longBytes), 64.bits)

        val result = bitBuffer.getAll()
        assertContentEquals(
            byteArrayOf(
                0x01, 0x23, 0x45, 0x67, 0x89.toByte(),
                0x0A.toByte(), 0xBC.toByte(), 0xDE.toByte(), 0xF0.toByte()
            ), result.array()
        )
    }

    @Test
    fun `put 60 bits with 4 bit offset`() {
        val bitBuffer = BitBuffer.wrap(ByteArray(8))
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0)), 4.bits)

        val longValue = 0x1234567890ABCDEFL
        val longBytes = ByteBuffer.allocate(8).putLong(longValue)
        bitBuffer.put(longBytes.rewind(), 60.bits)

        val result = bitBuffer.getAll()
        assertContentEquals(
            byteArrayOf(
                0x02, 0x34, 0x56, 0x78, 0x90.toByte(),
                0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte()
            ), result.array()
        )
    }

    @Test
    fun `put 63 bits with 5 bit offset`() {
        val bitBuffer = BitBuffer.wrap(ByteArray(9))
        bitBuffer.put(ByteBuffer.wrap(byteArrayOf(0)), 5.bits)

        val longValue = 0b0_001_0010__0100_1000__1001_1010__1100_1101__1111_0111__0011_0001__0011_1100__1010_0101
        val longBytes = ByteBuffer.allocate(8).putLong(longValue)
        bitBuffer.put(longBytes.rewind(), 63.bits)

        val result = bitBuffer.getAll()
        val expectedValueLong = 0b0000_0001__0010_0100__1000_1001__1010_1100__1101_1111__0111_0011__0001_0011__1100_1010
        val buff = ByteBuffer.allocate(9).putLong(expectedValueLong).put(0b0101_0000)
        assertContentEquals(buff.array(), result.array())
    }
}
