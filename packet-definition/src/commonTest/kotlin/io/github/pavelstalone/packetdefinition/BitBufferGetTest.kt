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
import io.github.pavelstalone.core.DataSize
import io.github.pavelstalone.core.DataSize.Companion.bits
import io.github.pavelstalone.core.DataSize.Companion.bytes
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class BitBufferGetTest {

    private fun BitBuffer.extract(size: DataSize): ByteArray {
        val buff = get(size)
        val result = ByteArray(buff.remaining())
        buff.get(result)
        return result
    }

    private val testBytes = byteArrayOf(
        0b1001_1101.toByte(),
        0b0011_1010.toByte(),
        0b0011_1010.toByte(),
    )

    @Test
    fun `example - minus byte with right shift`() {
        val bitBuffer = BitBuffer.wrap(testBytes)
        val buff = bitBuffer.get(5.bits)
        val result = ByteArray(buff.remaining())
        buff.get(result)

        assertEquals(listOf((-13).toByte()), result.toList())
    }

    @Test
    fun `example - fast byte get`() {
        val bitBuffer = BitBuffer.wrap(testBytes)
        val buff = bitBuffer.get(1.bytes)
        val result = ByteArray(buff.remaining())
        buff.get(result)

        assertEquals(listOf(0b1001_1101.toByte()), result.toList())
    }

    @Test
    fun `example - byte with left shift`() {
        val bitBuffer = BitBuffer.wrap(testBytes)
        bitBuffer.get(2.bits)
        val buff = bitBuffer.get(1.bytes)
        val result = ByteArray(buff.remaining())
        buff.get(result)

        assertEquals(listOf(0b0111_0100.toByte()), result.toList())
    }

    @Test
    fun `example - byte with right shift`() {
        val bitBuffer = BitBuffer.wrap(testBytes)
        bitBuffer.get(2.bits)
        val buff = bitBuffer.get(2.bits)
        val result = ByteArray(buff.remaining())
        buff.get(result)

        assertEquals(listOf(0b0000_0001.toByte()), result.toList())
    }

    @Test
    fun `sign ext - 1 bit, value 1`() {
        val data = byteArrayOf(0b1000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(1.bits)
        assertEquals(1, result.size)
        assertEquals((-1).toByte(), result[0])
    }

    @Test
    fun `sign ext - 1 bit, value 0`() {
        val data = byteArrayOf(0b0111_1111.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(1.bits)
        assertEquals(1, result.size)
        assertEquals(0x00.toByte(), result[0])
    }

    @Test
    fun `sign ext - 2 bits, value 10`() {
        val data = byteArrayOf(0b1000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(2.bits)
        assertEquals(1, result.size)
        assertEquals((-2).toByte(), result[0])
    }

    @Test
    fun `sign ext - 2 bits, value 01`() {
        val data = byteArrayOf(0b0100_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(2.bits)
        assertEquals(1, result.size)
        assertEquals(0x01.toByte(), result[0])
    }

    @Test
    fun `sign ext - 3 bits, value 110`() {
        val data = byteArrayOf(0b1100_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(3.bits)
        assertEquals(1, result.size)
        assertEquals((-2).toByte(), result[0])
    }

    @Test
    fun `sign ext - 3 bits, value 010`() {
        val data = byteArrayOf(0b0100_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(3.bits)
        assertEquals(1, result.size)
        assertEquals(0x02.toByte(), result[0])
    }

    @Test
    fun `sign ext - 5 bits, value 10011`() {
        val data = byteArrayOf(0b1001_1000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(5.bits)
        assertEquals(1, result.size)
        assertEquals(0b1111_0011.toByte(), result[0])
    }

    @Test
    fun `sign ext - 5 bits, value 00011`() {
        val data = byteArrayOf(0b0001_1000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(5.bits)
        assertEquals(1, result.size)
        assertEquals(0x03.toByte(), result[0])
    }

    @Test
    fun `sign ext - 7 bits, value 1111110`() {
        val data = byteArrayOf(0b1111_1100.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(7.bits)
        assertEquals(1, result.size)
        assertEquals((-2).toByte(), result[0])
    }

    @Test
    fun `sign ext - 7 bits, value 0000001`() {
        val data = byteArrayOf(0b0000_0010.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(7.bits)
        assertEquals(1, result.size)
        assertEquals(0x01.toByte(), result[0])
    }

    @Test
    fun `sign ext - all ones 5 bits`() {
        val data = byteArrayOf(0b1111_1000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(5.bits)
        assertEquals(1, result.size)
        assertEquals((-1).toByte(), result[0])
    }

    @Test
    fun `sign ext - all zeros 5 bits`() {
        val data = byteArrayOf(0b0000_0111.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(5.bits)
        assertEquals(1, result.size)
        assertEquals(0x00.toByte(), result[0])
    }

    @Test
    fun `byte path - 1 byte aligned (fast path)`() {
        val data = byteArrayOf(0b1010_1010.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(1.bytes)
        assertEquals(listOf(0b10101010.toByte()), result.toList())
    }

    @Test
    fun `byte path - offset 1, read 1 byte (left shift 1)`() {
        val data = byteArrayOf(0b0111_1111.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(1.bytes)
        assertEquals(listOf(0b1111_1110.toByte()), result.toList())
    }

    @Test
    fun `byte path - offset 3, read 1 byte (left shift 3)`() {
        val data = byteArrayOf(0b0001_1111.toByte(), 0b1110_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(3.bits)

        val result = bitBuffer.extract(1.bytes)
        assertEquals(listOf(0b1111_1111.toByte()), result.toList())
    }

    @Test
    fun `byte path - offset 5, read 1 byte (left shift 5)`() {
        val data = byteArrayOf(0b0000_0111.toByte(), 0b1111_1000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(5.bits)

        val result = bitBuffer.extract(1.bytes)
        assertEquals(listOf(0b1111_1111.toByte()), result.toList())
    }

    @Test
    fun `byte path - offset 7, read 1 byte (left shift 7)`() {
        val data = byteArrayOf(0b0000_0001.toByte(), 0b1111_1110.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(7.bits)

        val result = bitBuffer.extract(1.bytes)
        assertEquals(listOf(0b1111_1111.toByte()), result.toList())
    }

    @Test
    fun `byte path right - offset 2, read 2 bits`() {
        val data = byteArrayOf(0b1001_1111.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(2.bits)

        val result = bitBuffer.extract(2.bits)
        assertEquals(1, result.size)
        assertEquals(0b0000_0001.toByte(), result[0])
    }

    @Test
    fun `byte path right - offset 2, read 6 bits`() {
        val data = byteArrayOf(0b1001_1111.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(2.bits)

        val result = bitBuffer.extract(6.bits)
        assertEquals(1, result.size)
        assertEquals(0b0001_1111.toByte(), result[0])
    }

    @Test
    fun `byte path right - offset 5, read 3 bits`() {
        val data = byteArrayOf(0b0000_0111.toByte(), 0b1100_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(5.bits)

        val result = bitBuffer.extract(3.bits)
        assertEquals(1, result.size)
        assertEquals((-1).toByte(), result[0])
    }

    @Test
    fun `byte path right - offset 3, read 5 bits, negative value`() {
        val data = byteArrayOf(0b0001_0101.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(3.bits)

        val result = bitBuffer.extract(5.bits)
        assertEquals(1, result.size)
        assertEquals(0b1111_0101.toByte(), result[0])
    }

    @Test
    fun `byte path right - offset 1, read 7 bits, negative value`() {
        val data = byteArrayOf(0b0111_1110.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(7.bits)
        assertEquals(1, result.size)
        assertEquals((-2).toByte(), result[0])
    }

    @Test
    fun `byte path right - offset 1, read 7 bits, positive value`() {
        val data = byteArrayOf(0b0011_1110.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(7.bits)
        assertEquals(1, result.size)
        assertEquals(0b00111_110.toByte(), result[0])
    }

    @Test
    fun `byte path right with left tail - offset 1, read 2 bytes`() {
        val data = byteArrayOf(0b0111_1111.toByte(), 0b0000_0000.toByte(), 0b0111_1111.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(2.bytes)
        assertEquals(listOf(0b1111_1110.toByte(), 0b0000_0000.toByte()), result.toList())
    }

    @Test
    fun `byte path right with left tail - offset 5, read 2 bytes`() {
        val data = byteArrayOf(0b0000_0111.toByte(), 0b1111_1000.toByte(), 0b0011_1111.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(5.bits)

        val result = bitBuffer.extract(2.bytes)
        assertEquals(listOf(0b1111_1111.toByte(), 0b0000_0111.toByte()), result.toList())
    }

    @Test
    fun `byte path right multi - offset 2, read 10 bits, with left tail in 2nd byte`() {
        val data = byteArrayOf(0b1001_1111.toByte(), 0b0011_1111.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(2.bits)

        val result = bitBuffer.extract(10.bits)
        assertEquals(2, result.size)
        assertEquals(0b0000_0001.toByte(), result[0])
        assertEquals(0b1111_0011.toByte(), result[1])
    }

    @Test
    fun `byte path right sign - offset 0, read 3 bits from negative byte`() {
        val data = byteArrayOf(0b1010_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(3.bits)
        assertEquals(1, result.size)
        assertEquals((-3).toByte(), result[0])
    }

    @Test
    fun `byte path right sign - offset 0, read 3 bits, positive`() {
        val data = byteArrayOf(0b0100_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(3.bits)
        assertEquals(1, result.size)
        assertEquals(0x02.toByte(), result[0])
    }

    @Test
    fun `no shift - offset 3, read 5 bits`() {
        val data = byteArrayOf(0b0001_0101.toByte(), 0b0100_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(3.bits)

        val result = bitBuffer.extract(5.bits)
        assertEquals(1, result.size)
        assertEquals(0b1111_0101.toByte(), result[0])
    }

    @Test
    fun `no shift - offset 2, read 6 bits`() {
        val data = byteArrayOf(0b0011_0011.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(2.bits)

        val result = bitBuffer.extract(6.bits)
        assertEquals(1, result.size)
        assertEquals(0b1111_0011.toByte(), result[0])
    }

    @Test
    fun `no shift - offset 5, read 3 bits`() {
        val data = byteArrayOf(0b0000_0111.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(5.bits)

        val result = bitBuffer.extract(3.bits)
        assertEquals(1, result.size)
        assertEquals((-1).toByte(), result[0])
    }

    @Test
    fun `no shift - offset 4, read 4 bits`() {
        val data = byteArrayOf(0b0000_1010.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(4.bits)

        val result = bitBuffer.extract(4.bits)
        assertEquals(1, result.size)
        assertEquals((-6).toByte(), result[0])
    }

    @Test
    fun `no shift - offset 1, read 7 bits`() {
        val data = byteArrayOf(0b1010_1010.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(7.bits)
        assertEquals(1, result.size)
        assertEquals(0b0010_1010.toByte(), result[0])
    }

    @Test
    fun `no shift - offset 6, read 2 bits`() {
        val data = byteArrayOf(0b0000_0011.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(6.bits)

        val result = bitBuffer.extract(2.bits)
        assertEquals(1, result.size)
        assertEquals((-1).toByte(), result[0])
    }

    @Test
    fun `no shift - offset 7, read 1 bit`() {
        val data = byteArrayOf(0b0000_0001.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(7.bits)

        val result = bitBuffer.extract(1.bits)
        assertEquals(1, result.size)
        assertEquals(0b1111_1111.toByte(), result[0])
    }

    @Test
    fun `int path - 4 bytes aligned (fast path)`() {
        val data = byteArrayOf(
            0b1010_1010.toByte(),
            0b1100_1100.toByte(),
            0b1111_0000.toByte(),
            0b0000_1111.toByte(),
            0b0101_0101.toByte()
        )
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(4.bytes)
        assertEquals(
            listOf(0xAA.toByte(), 0xCC.toByte(), 0xF0.toByte(), 0x0F.toByte()),
            result.toList()
        )
    }

    @Test
    fun `int path - offset 1, read 4 bytes (left shift 1)`() {
        val data = byteArrayOf(
            0b1111_1111.toByte(),
            0b0000_0000.toByte(),
            0b0000_0000.toByte(),
            0b1111_1111.toByte(),
            0b1111_1111.toByte()
        )
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(4.bytes)
        assertEquals(4, result.size)
        assertEquals(0xFE, result[0].toInt() and 0xFF)
        assertEquals(0x00, result[1].toInt() and 0xFF)
        assertEquals(0x01, result[2].toInt() and 0xFF)
        assertEquals(0xFF, result[3].toInt() and 0xFF)
    }

    @Test
    fun `int path - offset 3, read 4 bytes (left shift 3)`() {
        val data = byteArrayOf(
            0b1111_1111.toByte(),
            0b0000_0000.toByte(),
            0b1111_1111.toByte(),
            0b0000_0000.toByte(),
            0b1111_1111.toByte()
        )
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(3.bits)

        val result = bitBuffer.extract(4.bytes)
        assertEquals(4, result.size)
        assertEquals(0xF8, result[0].toInt() and 0xFF)
        assertEquals(0x07, result[1].toInt() and 0xFF)
        assertEquals(0xF8, result[2].toInt() and 0xFF)
        assertEquals(0x07, result[3].toInt() and 0xFF)
    }

    @Test
    fun `int path - offset 1, read 5 bytes (Int then Byte)`() {
        val data =
            byteArrayOf(
                0b1111_1111.toByte(),
                0b0000_0000.toByte(),
                0b1111_1111.toByte(),
                0b0000_0000.toByte(),
                0b1111_1111.toByte(),
                0b1111_1111.toByte()
            )
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(5.bytes)
        assertEquals(5, result.size)
        assertEquals(0xFE, result[0].toInt() and 0xFF)
        assertEquals(0x01, result[1].toInt() and 0xFF)
        assertEquals(0xFE, result[2].toInt() and 0xFF)
        assertEquals(0x01, result[3].toInt() and 0xFF)
        assertEquals(0xFF, result[4].toInt() and 0xFF)
    }

    @Test
    fun `int path - offset 2, read 33 bits (right shift, Int then Byte)`() {
        val data = ByteArray(6) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(2.bits)

        val result = bitBuffer.extract(33.bits)
        assertEquals(5, result.size)
        for (i in 0 until 5) {
            assertEquals(0xFF, result[i].toInt() and 0xFF, "Byte $i")
        }
    }

    @Test
    fun `int path - offset 2, read 31 bits (right shift, Int then Byte, sign ext)`() {
        val data = byteArrayOf(
            0b1111_1111.toByte(),
            0b0000_0000.toByte(),
            0b0000_0000.toByte(),
            0b0000_0000.toByte(),
            0b0000_0000.toByte()
        )
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(2.bits)

        val result = bitBuffer.extract(31.bits)
        assertEquals(4, result.size)
        assertEquals(0xFE, result[0].toInt() and 0xFF)
        assertEquals(0x00, result[1].toInt() and 0xFF)
        assertEquals(0x00, result[2].toInt() and 0xFF)
        assertEquals(0x00, result[3].toInt() and 0xFF)
    }

    @Test
    fun `long path - 8 bytes aligned (fast path)`() {
        val data = ByteArray(16) { (it and 0xFF).toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(8.bytes)
        assertEquals(8, result.size)
        for (i in 0 until 8) {
            assertEquals(i, result[i].toInt() and 0xFF)
        }
    }

    @Test
    fun `long path - offset 1, read 8 bytes (left shift 1)`() {
        val data = ByteArray(16) { (it and 0xFF).toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(8.bytes)
        assertEquals(8, result.size)
        assertEquals(0, result[0].toInt() and 0xFF)
        assertEquals(2, result[1].toInt() and 0xFF)
        assertEquals(4, result[2].toInt() and 0xFF)
        assertEquals(6, result[3].toInt() and 0xFF)
        assertEquals(8, result[4].toInt() and 0xFF)
        assertEquals(10, result[5].toInt() and 0xFF)
        assertEquals(12, result[6].toInt() and 0xFF)
        assertEquals(14, result[7].toInt() and 0xFF)
    }

    @Test
    fun `long path - offset 5, read 8 bytes (left shift 5)`() {
        val data = ByteArray(16) { (it and 0xFF).toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(5.bits)

        val result = bitBuffer.extract(8.bytes)
        assertEquals(8, result.size)
        assertEquals(0, result[0].toInt() and 0xFF)
        assertEquals(32, result[1].toInt() and 0xFF)
        assertEquals(64, result[2].toInt() and 0xFF)
        assertEquals(96, result[3].toInt() and 0xFF)
        assertEquals(0b1000_0000, result[4].toInt() and 0xFF)
        assertEquals(0b1010_0000, result[5].toInt() and 0xFF)
        assertEquals(0b1100_0000, result[6].toInt() and 0xFF)
        assertEquals(0b1110_0001, result[7].toInt() and 0xFF)
    }

    @Test
    fun `long path - offset 1, read 9 bytes (Long then Byte)`() {
        val data = ByteArray(16) { (it and 0xFF).toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(9.bytes)
        assertEquals(9, result.size)
        assertEquals(0, result[0].toInt() and 0xFF)
        assertEquals(2, result[1].toInt() and 0xFF)
        assertEquals(4, result[2].toInt() and 0xFF)
        assertEquals(6, result[3].toInt() and 0xFF)
        assertEquals(8, result[4].toInt() and 0xFF)
        assertEquals(10, result[5].toInt() and 0xFF)
        assertEquals(12, result[6].toInt() and 0xFF)
        assertEquals(14, result[7].toInt() and 0xFF)
        assertEquals(16, result[8].toInt() and 0xFF)
    }

    @Test
    fun `long path right - offset 1, read 73 bits (Long then Byte, sign ext)`() {
        val data = ByteArray(16) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(73.bits)
        assertEquals(10, result.size)

        for (i in 0 until 10) {
            assertEquals(0xFF, result[i].toInt() and 0xFF, "Byte $i")
        }
    }

    @Test
    fun `long path right - offset 1, read 73 bits, zeros (sign ext zero)`() {
        val data = ByteArray(16)
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(73.bits)
        assertEquals(10, result.size)

        for (i in 0 until 10) {
            assertEquals(0x00, result[i].toInt(), "Byte $i")
        }
    }

    @Test
    fun `sign ext multi-byte - 9 bits, all ones`() {
        val data = ByteArray(4) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(9.bits)
        assertEquals(2, result.size)
        assertEquals(0xFF, result[0].toInt() and 0xFF)
        assertEquals(0xFF, result[1].toInt() and 0xFF)
    }

    @Test
    fun `sign ext multi-byte - 9 bits, zero then one`() {
        val data = byteArrayOf(0b0000_0001.toByte(), 0b1000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(9.bits)
        assertEquals(2, result.size)
        assertEquals(0, result[0].toInt() and 0xFF)
        assertEquals(3, result[1].toInt() and 0xFF)
    }

    @Test
    fun `sign ext multi-byte - 9 bits, zero then zero`() {
        val data = byteArrayOf(0b0000_0001.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(9.bits)
        assertEquals(2, result.size)
        assertEquals(0, result[0].toInt() and 0xFF)
        assertEquals(2, result[1].toInt() and 0xFF)
    }

    @Test
    fun `sign ext multi-byte - 17 bits, negative head`() {
        val data = byteArrayOf(0b1001_1101.toByte(), 0b0011_1010.toByte(), 0b0011_1010.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(17.bits)
        assertEquals(3, result.size)
        assertEquals(0b1111_1111, result[0].toInt() and 0xFF)
        assertEquals(0b0011_1010, result[1].toInt() and 0xFF)
        assertEquals(0b0111_0100, result[2].toInt() and 0xFF)
    }

    @Test
    fun `sign ext multi-byte - 15 bits, last bit is 1`() {
        val data = ByteArray(4) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(15.bits)
        assertEquals(2, result.size)
        assertEquals(0xFF, result[0].toInt() and 0xFF)
        assertEquals(0xFF, result[1].toInt() and 0xFF)  // (1)111111
    }

    @Test
    fun `sign ext multi-byte - 33 bits`() {
        val data = ByteArray(8) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(33.bits)
        assertEquals(5, result.size)
        for (i in 0 until 4) {
            assertEquals(0xFF, result[i].toInt() and 0xFF)
        }
        assertEquals(0xFF, result[4].toInt() and 0xFF)
    }

    @Test
    fun `sign ext multi-byte - 65 bits, all FF`() {
        val data = ByteArray(16) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(65.bits)
        assertEquals(9, result.size)
        for (i in 0 until 9) {
            assertEquals(0xFF, result[i].toInt() and 0xFF, "Byte $i")
        }
    }

    @Test
    fun `sequential - consume all 16 bits one by one`() {
        val data = byteArrayOf(0b1010_1010.toByte(), 0b0101_0101.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val expectedBits = listOf(1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1)
        for (i in expectedBits.indices) {
            val result = bitBuffer.extract(1.bits)
            val actualBit = (result[0].toInt() shr 7) and 1
            assertEquals(expectedBits[i], actualBit, "Bit $i")
        }
    }

    @Test
    fun `boundary - 7 bits`() {
        val data = byteArrayOf(0b1111_1110.toByte(), 0b0000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(7.bits)
        assertEquals(1, result.size)
        assertEquals((-1).toByte(), result[0])
    }

    @Test
    fun `boundary - 8 bits (fast path)`() {
        val data = byteArrayOf(0b1010_1010.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(8.bits)
        assertEquals(listOf(0xAA.toByte()), result.toList())
    }

    @Test
    fun `boundary - 9 bits`() {
        val data = byteArrayOf(0b1111_1111.toByte(), 0b1000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(9.bits)
        assertEquals(2, result.size)
        assertEquals(0xFF, result[0].toInt() and 0xFF)
        assertEquals(0xFF, result[1].toInt() and 0xFF)
    }

    @Test
    fun `boundary - 16 bits (fast path)`() {
        val data = byteArrayOf(0b1010_1010.toByte(), 0b0101_0101.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(16.bits)
        assertEquals(listOf(0xAA.toByte(), 0x55.toByte()), result.toList())
    }

    @Test
    fun `boundary - 17 bits`() {
        val data = ByteArray(4) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(17.bits)
        assertEquals(3, result.size)
        assertEquals(0xFF, result[0].toInt() and 0xFF)
        assertEquals(0xFF, result[1].toInt() and 0xFF)
        assertEquals(0xFF, result[2].toInt() and 0xFF)
    }

    @Test
    fun `boundary - 31 bits`() {
        val data = ByteArray(8) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(31.bits)
        assertEquals(4, result.size)
        assertEquals(0xFF, result[0].toInt() and 0xFF)
        assertEquals(0xFF, result[1].toInt() and 0xFF)
        assertEquals(0xFF, result[2].toInt() and 0xFF)
        assertEquals(0xFF, result[3].toInt() and 0xFF)
    }

    @Test
    fun `boundary - 32 bits (fast path)`() {
        val data = ByteArray(8) { ((it * 37) and 0xFF).toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(32.bits)
        assertEquals(4, result.size)
        for (i in 0 until 4) {
            assertEquals(data[i], result[i])
        }
    }

    @Test
    fun `boundary - 33 bits`() {
        val data = ByteArray(8) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(33.bits)
        assertEquals(5, result.size)
        assertEquals(0xFF, result[0].toInt() and 0xFF)
        assertEquals(0xFF, result[1].toInt() and 0xFF)
        assertEquals(0xFF, result[2].toInt() and 0xFF)
        assertEquals(0xFF, result[3].toInt() and 0xFF)
        assertEquals(0xFF, result[4].toInt() and 0xFF)  // (1111111)1
    }

    @Test
    fun `boundary - 63 bits`() {
        val data = ByteArray(16) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(63.bits)
        assertEquals(8, result.size)
        for (i in 0 until 8) {
            assertEquals(0xFF, result[i].toInt() and 0xFF)
        }
    }

    @Test
    fun `boundary - 64 bits (fast path)`() {
        val data = ByteArray(16) { ((it * 41) and 0xFF).toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(8.bytes)
        assertEquals(8, result.size)
        for (i in 0 until 8) {
            assertEquals(data[i], result[i])
        }
    }

    @Test
    fun `boundary - 65 bits`() {
        val data = ByteArray(16) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(65.bits)
        assertEquals(9, result.size)
        for (i in 0 until 9) {
            assertEquals(0xFF, result[i].toInt() and 0xFF)
        }
    }

    @Test
    fun `large data - 64 bytes all FF, offset 3, read 50 bytes (Long path)`() {
        val data = ByteArray(64) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(3.bits)

        val result = bitBuffer.extract(50.bytes)
        assertEquals(50, result.size)
        for (i in 0 until 50) {
            assertEquals(0xFF, result[i].toInt() and 0xFF, "Byte $i")
        }
    }

    @Test
    fun `large data - 64 bytes all zeros, offset 7, read 50 bytes`() {
        val data = ByteArray(64)
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(7.bits)

        val result = bitBuffer.extract(50.bytes)
        assertEquals(50, result.size)
        for (i in 0 until 50) {
            assertEquals(0x00, result[i].toInt(), "Byte $i")
        }
    }

    @Test
    fun `large data - checkered pattern, offset 5, read 32 bytes`() {
        val data = ByteArray(64) { if (it % 2 == 0) 0xAA.toByte() else 0x55.toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(5.bits)

        val result = bitBuffer.extract(32.bytes)
        assertEquals(32, result.size)

        val ref = BitBuffer.wrap(data)
        ref.get(5.bits)
        for (byteIdx in 0 until 32) {
            var expectedByte = 0
            for (bitIdx in 0 until 8) {
                val r = ref.extract(1.bits)
                val bit = (r[0].toInt() shr 7) and 1
                expectedByte = (expectedByte shl 1) or bit
            }
            assertEquals(
                expectedByte, result[byteIdx].toInt() and 0xFF,
                "Checkered byte $byteIdx"
            )
        }
    }

    @Test
    fun `stress - all offsets 0-7 with 16-byte aligned reads`() {
        val data = ByteArray(32) { ((it * 7 + 13) and 0xFF).toByte() }

        for (offset in 0..7) {
            val bitBuffer = BitBuffer.wrap(data)
            bitBuffer.get(offset.bits)

            val result = bitBuffer.extract(16.bytes)
            assertEquals(16, result.size)

            val ref = BitBuffer.wrap(data)
            ref.get(offset.bits)
            for (byteIdx in 0 until 16) {
                var expected = 0
                for (b in 0 until 8) {
                    val r = ref.extract(1.bits)
                    expected = (expected shl 1) or ((r[0].toInt() shr 7) and 1)
                }
                assertEquals(
                    expected, result[byteIdx].toInt() and 0xFF,
                    "Offset=$offset, byte=$byteIdx"
                )
            }
        }
    }

    @Test
    fun `single byte - read 1 bit (sign ext)`() {
        val data = byteArrayOf(0b1000_0000.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(1.bits)
        assertEquals((-1).toByte(), result[0])
    }

    @Test
    fun `single byte - read all 8 bits (fast path)`() {
        val data = byteArrayOf(0b1100_1100.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(1.bytes)
        assertEquals(listOf(0xCC.toByte()), result.toList())
    }

    @Test
    fun `two bytes - read 15 bits, sign ext last byte`() {
        val data = byteArrayOf(0b1111_1111.toByte(), 0b1111_1110.toByte())
        val bitBuffer = BitBuffer.wrap(data)

        val result = bitBuffer.extract(15.bits)
        assertEquals(2, result.size)
        assertEquals(0xFF, result[0].toInt() and 0xFF)
        assertEquals(0xFF, result[1].toInt() and 0xFF)
    }

    @Test
    fun `all byte values - aligned read 256 bytes`() {
        val data = ByteArray(256) { it.toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        for (i in 0 until 256) {
            val result = bitBuffer.extract(1.bytes)
            assertEquals(1, result.size)
            assertEquals(data[i], result[0], "Byte at index $i")
        }
    }

    @Test
    fun `all byte values - offset 1, read 254 bytes`() {
        val data = ByteArray(256) { it.toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        for (i in 0 until 254) {
            val result = bitBuffer.extract(1.bytes)
            assertEquals(1, result.size)
            val b0 = data[i].toInt() and 0xFF
            val b1 = data[i + 1].toInt() and 0xFF
            val expected = ((b0 shl 1) or (b1 shr 7)) and 0xFF
            assertEquals(
                expected, result[0].toInt() and 0xFF,
                "Offset-1 byte at index $i"
            )
        }
    }

    @Test
    fun `all byte values - offset 3, read 253 bytes`() {
        val data = ByteArray(256) { it.toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(3.bits)

        for (i in 0 until 253) {
            val result = bitBuffer.extract(1.bytes)
            assertEquals(1, result.size)
            val b0 = data[i].toInt() and 0xFF
            val b1 = data[i + 1].toInt() and 0xFF
            val expected = ((b0 shl 3) or (b1 shr 5)) and 0xFF
            assertEquals(
                expected, result[0].toInt() and 0xFF,
                "Offset-3 byte at index $i"
            )
        }
    }

    @Test
    fun `all byte values - offset 5, read 252 bytes`() {
        val data = ByteArray(256) { it.toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(5.bits)

        for (i in 0 until 252) {
            val result = bitBuffer.extract(1.bytes)
            assertEquals(1, result.size)
            val b0 = data[i].toInt() and 0xFF
            val b1 = data[i + 1].toInt() and 0xFF
            val expected = ((b0 shl 5) or (b1 shr 3)) and 0xFF
            assertEquals(
                expected, result[0].toInt() and 0xFF,
                "Offset-5 byte at index $i"
            )
        }
    }

    @Test
    fun `all byte values - offset 7, read 251 bytes`() {
        val data = ByteArray(256) { it.toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(7.bits)

        for (i in 0 until 251) {
            val result = bitBuffer.extract(1.bytes)
            assertEquals(1, result.size)
            val b0 = data[i].toInt() and 0xFF
            val b1 = data[i + 1].toInt() and 0xFF
            val expected = ((b0 shl 7) or (b1 shr 1)) and 0xFF
            assertEquals(
                expected, result[0].toInt() and 0xFF,
                "Offset-7 byte at index $i"
            )
        }
    }

    @Test
    fun `overflow - all FF with Long shift offset 1`() {
        val data = ByteArray(16) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(8.bytes)
        assertEquals(8, result.size)
        for (i in 0 until 8) {
            assertEquals(
                0xFF, result[i].toInt() and 0xFF,
                "FF Long offset=1 byte $i"
            )
        }
    }

    @Test
    fun `overflow - all FF with Int shift offset 1`() {
        val data = ByteArray(8) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(4.bytes)
        assertEquals(4, result.size)
        for (i in 0 until 4) {
            assertEquals(
                0xFF, result[i].toInt() and 0xFF,
                "FF Int offset=1 byte $i"
            )
        }
    }

    @Test
    fun `overflow - all FF with Byte shift offset 1`() {
        val data = ByteArray(4) { 0xFF.toByte() }
        val bitBuffer = BitBuffer.wrap(data)
        bitBuffer.get(1.bits)

        val result = bitBuffer.extract(2.bytes)
        assertEquals(2, result.size)
        for (i in 0 until 2) {
            assertEquals(
                0xFF, result[i].toInt() and 0xFF,
                "FF Byte offset=1 byte $i"
            )
        }
    }

    @Test
    fun `mirror - incrementing bytes reconstructed bit by bit`() {
        val data = ByteArray(8) { it.toByte() }
        val bitBuffer = BitBuffer.wrap(data)

        var reconstructed = 0L
        for (i in 0 until 64) {
            val r = bitBuffer.extract(1.bits)
            val bit = (r[0].toInt() shr 7) and 1
            reconstructed = (reconstructed shl 1) or bit.toULong().toLong()
        }

        assertEquals(ByteBuffer.wrap(data).getLong(), reconstructed)
    }

    @Test
    fun `mirror - fibonacci-like bytes`() {
        val data = byteArrayOf(
            0b00000001, 0b00000011, 0b00000100, 0b00000111,
            0b00001011, 0b00010010, 0b00011001, 0b00101011,
        )
        val bitBuffer = BitBuffer.wrap(data)

        var reconstructed = 0L
        for (i in 0 until 64) {
            val r = bitBuffer.extract(1.bits)
            val bit = (r[0].toInt() shr 7) and 1
            reconstructed = (reconstructed shl 1) or bit.toULong().toLong()
        }

        assertEquals(ByteBuffer.wrap(data).getLong(), reconstructed)
    }
}
