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

import io.github.pavelstalone.packetdefinition.packet.packet
import io.github.pavelstalone.core.DataSize.Companion.bits
import io.github.pavelstalone.core.DataSize.Companion.bytes
import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PacketParseTest {

    @Test
    fun `parse basic types`() {
        val data = byteArrayOf(
            0x01, // byte
            0x00, 0x00, 0x00, 0x02, // int
            0x3F, 0x80.toByte(), 0x00, 0x00, // float (1.0)
            0xFF.toByte(), // boolean (true)
            0x41, 0x42, 0x43, // string "ABC"
            0x04, 0x05 // bytes
        )

        val result = packet {
            val b = byte()
            val i = int()
            val f = float(4.bytes)
            val bool = boolean(8.bits)
            val s = string(3.bytes, Charset.forName("UTF-8"))
            val arr = bytes(2.bytes)

            Triple(b.value, i.value, f.value) to Triple(bool.value, s.value, arr.value)
        }.fill(data)

        assertEquals(0x01.toByte(), result.first.first)
        assertEquals(2, result.first.second)
        assertEquals(1.0f, result.first.third)
        assertEquals(true, result.second.first)
        assertEquals("ABC", result.second.second)
        assertContentEquals(byteArrayOf(0x04, 0x05), result.second.third)
    }

    @Test
    fun `parse bit alignment`() {
        // 0b101 01010 -> 0b101 (-3) and 0b01010 (10)
        // 0b10101010 = 0xAA
        val data = byteArrayOf(0xAA.toByte())

        val result = packet {
            val first = int(3.bits)
            val second = int(5.bits)
            first.value to second.value
        }.fill(data)

        assertEquals(-3, result.first)
        assertEquals(10, result.second)
    }

    @Test
    fun `parse across byte boundaries`() {
        // 0b111111 11 | 10000000
        val data = byteArrayOf(0xFF.toByte(), 0x80.toByte())

        val result = packet {
            val first = int(6.bits) // 0b111111 = -1
            val second = int(4.bits) // 0b11 10 = -2
            first.value to second.value
        }.fill(data)

        assertEquals(-1, result.first)
        assertEquals(-2, result.second)
    }

    @Test
    fun `parse nested structures`() {
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val innerPacket = packet {
            val a = byte()
            val b = byte()
            a.value to b.value
        }

        val result = packet {
            val outer = bytes(2.bytes)
            val nested1 = from(outer) {
                val x = byte()
                val y = byte()
                x.value to y.value
            }
            val nested2 = from(bytes(2.bytes), innerPacket)
            nested1 to nested2
        }.fill(data)

        assertEquals(0x01.toByte(), result.first.first)
        assertEquals(0x02.toByte(), result.first.second)
        assertEquals(0x03.toByte(), result.second.first)
        assertEquals(0x04.toByte(), result.second.second)
    }

    @Test
    fun `parse nested structures without whole bytes`() {
        val data = byteArrayOf(0b0010_1001, 0b1010_0011.toByte())

        val innerPacket = packet {
            val a = boolean()
            val b = int(5.bits)
            a.value to b.value
        }

        val result = packet {
            reserve(1.bits)
            val nested1 = from(bytes(7.bits)) {
                val x = int(3.bits)
                val y = int(4.bits)
                x.value to y.value
            }
            reserve(2.bits)
            val nested2 = from(bytes(remainingSize), innerPacket)
            nested1 to nested2
        }.fill(data)

        assertEquals(2, result.first.first)
        assertEquals(-7, result.first.second)
        assertEquals(true, result.second.first)
        assertEquals(3, result.second.second)
    }

    @Test
    fun `reserve and dynamic size`() {
        val data = byteArrayOf(0x01, 0xFF.toByte(), 0xAA.toByte(), 0x03)

        val result = packet {
            val size = byte()
            reserve(1.bytes) // skip 0xFF
            val payload = bytes(size.value.toInt().bytes)
            val last = byte()
            payload.value to last.value
        }.fill(data)

        assertContentEquals(byteArrayOf(0xAA.toByte()), result.first)
        assertEquals(0x03.toByte(), result.second)
    }

    @Test
    fun `parse empty buffer fails`() {
        assertFailsWith<Exception> {
            packet { byte() }.fill(byteArrayOf())
        }
    }

    @Test
    fun `parse beyond bounds fails`() {
        assertFailsWith<Exception> {
            packet { int(8.bytes) }.fill(byteArrayOf(0x01))
        }
    }

    @Test
    fun `zero size field`() {
        val data = byteArrayOf(0x01)
        val result = packet {
            val empty = bytes(0.bytes)
            val b = byte()
            empty.value to b.value
        }.fill(data)

        assertEquals(0, result.first.size)
        assertEquals(0x01.toByte(), result.second)
    }
}
