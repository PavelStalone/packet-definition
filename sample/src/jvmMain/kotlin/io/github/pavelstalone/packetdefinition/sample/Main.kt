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

package io.github.pavelstalone.packetdefinition.sample

import io.github.pavelstalone.core.DataSize.Companion.bits
import io.github.pavelstalone.core.DataSize.Companion.bytes
import io.github.pavelstalone.packetdefinition.packet.fillPacket
import io.github.pavelstalone.packetdefinition.packet.packet
import io.github.pavelstalone.packetdefinition.sample.SimplePacket.Info.Connection
import io.github.pavelstalone.packetdefinition.sample.SimplePacket.Info.Data
import io.github.pavelstalone.packetdefinition.sample.SimplePacket.Ping
import io.github.pavelstalone.packetdefinition.sample.SimplePacket.Reconnect
import io.github.pavelstalone.packetdefinition.sample.SimplePacket.SerialNumber
import io.github.pavelstalone.packetdefinition.sample.SimplePacket.Unknown
import io.github.pavelstalone.packetdefinition.validation.CRC8Validation
import java.nio.ByteBuffer

fun main() {
    println(SimplePacket.definition.fill(MockSimplePacket.ping))
    println(SimplePacket.definition.fill(MockSimplePacket.data))
    println(SimplePacket.definition.fill(MockSimplePacket.unknown))
    println(SimplePacket.definition.fill(MockSimplePacket.reconnect))
    println(SimplePacket.definition.fill(MockSimplePacket.connection))
    println(SimplePacket.definition.fill(MockSimplePacket.serialNumber))

    val ping = fillPacket { byte(0x1) }
    val data = fillPacket {
        byte(0x45)
        val crc = CRC8Validation.calculate(
            boolean(true),
            int(4, 7.bits),
            float(12f)
        )
        byte(crc)
    }
    val unknown = fillPacket { byte(0x2) }
    val reconnect = fillPacket { byte(0x22) }
    val connection = fillPacket {
        byte(0x46)
        val id = "mock_id".toByteArray()
        int(id.size, 1.bytes)
        bytes(id)
        reserve(3.bits)
        boolean(true)
        boolean(true)
    }
    val serialNumber = fillPacket {
        byte(0x23)
        string("serialNumber")
    }

    println()
    println(ping.array().contentEquals(MockSimplePacket.ping))
    println(data.array().contentEquals(MockSimplePacket.data))
    println(unknown.array().contentEquals(MockSimplePacket.unknown))
    println(reconnect.array().contentEquals(MockSimplePacket.reconnect))
    println(connection.array().contentEquals(MockSimplePacket.connection))
    println(serialNumber.array().contentEquals(MockSimplePacket.serialNumber))

    println("\nParse by PacketDefinition")
    println(parseByPacketDefinition(ByteBuffer.wrap(MockSimplePacket.ping)))
    println(parseByPacketDefinition(ByteBuffer.wrap(MockSimplePacket.data)))
    println(parseByPacketDefinition(ByteBuffer.wrap(MockSimplePacket.unknown)))
    println(parseByPacketDefinition(ByteBuffer.wrap(MockSimplePacket.reconnect)))
    println(parseByPacketDefinition(ByteBuffer.wrap(MockSimplePacket.connection)))
    println(parseByPacketDefinition(ByteBuffer.wrap(MockSimplePacket.serialNumber)))

    println("\nParse by ByteBuffer")
    println(parseByByteBuffer(ByteBuffer.wrap(MockSimplePacket.ping)))
    println(parseByByteBuffer(ByteBuffer.wrap(MockSimplePacket.data)))
    println(parseByByteBuffer(ByteBuffer.wrap(MockSimplePacket.unknown)))
    println(parseByByteBuffer(ByteBuffer.wrap(MockSimplePacket.reconnect)))
    println(parseByByteBuffer(ByteBuffer.wrap(MockSimplePacket.connection)))
    println(parseByByteBuffer(ByteBuffer.wrap(MockSimplePacket.serialNumber)))
}

fun parseByPacketDefinition(bytes: ByteBuffer): SimplePacket = packet {
    val flag = int(1.bytes).value

    when (flag) {
        0x1 -> Ping
        0x22 -> Reconnect
        0x23 -> SerialNumber(value = string(remainingSize).value)
        0x45 -> {
            val isActive = boolean()
            val count = int(7.bits)
            val value = float(4.bytes)
            val crc = byte()

            require(CRC8Validation(crc.value).validate(isActive, count, value))

            Data(
                value = value.value,
                count = count.value,
                isActive = isActive.value,
            )
        }

        0x46 -> {
            val idSize = int(1.bytes).value
            val id = string(size = idSize.bytes).value
            reserve(3.bits)
            val isConnect = boolean().value
            val isBanned = boolean().value

            Connection(
                id = id,
                isBanned = isBanned,
                isConnect = isConnect,
            )
        }

        else -> Unknown
    }
}.fill(bytes)

fun parseByByteBuffer(bytes: ByteBuffer): SimplePacket {
    val flag = bytes.get().toInt()

    return when (flag) {
        0x1 -> Ping
        0x22 -> Reconnect
        0x23 -> {
            val serialArray = ByteArray(bytes.remaining())
            bytes.get(serialArray)

            SerialNumber(value = serialArray.toString(charset = Charsets.UTF_8))
        }

        0x45 -> {
            val forCrcCheck = bytes.slice().limit(5)
            val byte = bytes.get().toInt()
            val isActive = (byte and 0x80) != 0
            val count = (byte and 0x7F)
            val value = bytes.getFloat()
            val crc = bytes.get()

            require(CRC8Validation.calculate(forCrcCheck) == crc)

            Data(
                value = value,
                count = count,
                isActive = isActive,
            )
        }

        0x46 -> {
            val idSize = bytes.get().toInt()
            val idArray = ByteArray(idSize)
            bytes.get(idArray)
            val id = idArray.toString(charset = Charsets.UTF_8)

            val byte = bytes.get().toInt()
            val isConnect = (byte and 0x10) != 0
            val isBanned = (byte and 0x8) != 0

            Connection(
                id = id,
                isBanned = isBanned,
                isConnect = isConnect,
            )
        }

        else -> Unknown
    }
}
