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
import io.github.pavelstalone.packetdefinition.packet.Packet
import io.github.pavelstalone.packetdefinition.packet.PacketScope
import io.github.pavelstalone.packetdefinition.packet.packet
import io.github.pavelstalone.packetdefinition.validation.CRC8Validation

sealed interface SimplePacket {

    data object Ping : SimplePacket
    data object Reconnect : SimplePacket
    data object Unknown : SimplePacket

    data class SerialNumber(val value: String) : SimplePacket

    sealed interface Info : SimplePacket {

        data class Data(val value: Float, val count: Int, val isActive: Boolean) : Info
        data class Connection(val id: String, val isConnect: Boolean, val isBanned: Boolean) : Info

        companion object {

            val parseData: PacketScope.() -> Data = {
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

            val connectionDefinition = packet {
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
        }
    }

    companion object {

        val definition: Packet<SimplePacket> = packet {
            val flag = int(1.bytes).value

            when (flag) {
                0x1 -> Ping
                0x22 -> Reconnect
                0x23 -> SerialNumber(value = string(remainingSize).value)
                0x45 -> Info.parseData(this)
                0x46 -> from(bytes(remainingSize), Info.connectionDefinition)
                else -> Unknown
            }
        }
    }
}
