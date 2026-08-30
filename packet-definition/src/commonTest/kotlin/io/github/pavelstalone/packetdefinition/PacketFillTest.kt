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

import io.github.pavelstalone.packetdefinition.packet.fillPacket
import io.github.pavelstalone.core.DataSize.Companion.bits
import io.github.pavelstalone.core.DataSize.Companion.bytes
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PacketFillTest {

    @Test
    fun `fill basic types`() {
        val buffer = fillPacket {
            byte(0x01.toByte())
            int(2)
            float(1.0f)
            boolean(true, 8.bits)
            string("ABC")
            bytes(byteArrayOf(0x04, 0x05))
        }
        
        val result = ByteArray(buffer.remaining())
        buffer.get(result)
        println("Basic types result: ${result.joinToString { it.toUByte().toString(16) }}")
        val expected = byteArrayOf(
            0x01,
            0x00, 0x00, 0x00, 0x02,
            0x3F, 0x80.toByte(), 0x00, 0x00,
            0xFF.toByte(),
            0x41, 0x42, 0x43,
            0x04, 0x05
        )

        assertContentEquals(expected, result)
    }

    @Test
    fun `fill bit packing`() {
        val buffer = fillPacket {
            int(5, 3.bits)
            int(10, 5.bits)
        }

        val result = ByteArray(buffer.remaining())
        buffer.get(result)
        println("Bit packing result: ${result.joinToString { it.toUByte().toString(16) }}")
        assertContentEquals(byteArrayOf(0xAA.toByte()), result)
    }

    @Test
    fun `fill buffer expansion`() {
        val buffer = fillPacket {
            bytes(ByteArray(10) { it.toByte() })
        }

        assertEquals(10, buffer.remaining())
        val result = ByteArray(buffer.remaining())
        buffer.get(result)
        assertContentEquals(ByteArray(10) { it.toByte() }, result)
    }

    @Test
    fun `fill reserve`() {
        val buffer = fillPacket {
            byte(0x01)
            reserve(2.bytes)
            byte(0x02)
        }

        val result = ByteArray(buffer.remaining())
        buffer.get(result)
        assertEquals(4, result.size)
        assertEquals(0x01.toByte(), result[0])
        assertEquals(0x00.toByte(), result[1])
        assertEquals(0x00.toByte(), result[2])
        assertEquals(0x02.toByte(), result[3])
    }

    @Test
    fun `fill custom`() {
        val buffer = fillPacket {
            custom("HI", 2.bytes) { value ->
                ByteBuffer.wrap(value.toByteArray())
            }
        }

        val result = ByteArray(buffer.remaining())
        buffer.get(result)
        assertContentEquals(byteArrayOf(0x48, 0x49), result)
    }

    @Test
    fun `fill bit spanning byte boundary`() {
        val buffer = fillPacket {
            int(63, 6.bits) // 111111
            int(14, 4.bits) // 1110
        }

        // 111111 11 | 10000000 -> 0xFF, 0x80
        val result = ByteArray(buffer.remaining())
        buffer.get(result)
        assertContentEquals(byteArrayOf(0xFF.toByte(), 0x80.toByte()), result)
    }
}
