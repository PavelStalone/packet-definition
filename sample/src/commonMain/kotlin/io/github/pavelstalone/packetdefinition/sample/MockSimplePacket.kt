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

import java.nio.ByteBuffer

object MockSimplePacket {

    val ping = byteArrayOf(0x1)
    val reconnect = byteArrayOf(0x22)
    val serialNumber = byteArrayOf(0x23, *"serialNumber".toByteArray())
    val data = byteArrayOf(
        0x45, // flag
        0b1000_0100.toByte(), // isActive and count
        *ByteBuffer.allocate(Float.SIZE_BYTES).putFloat(12f).array(), // value
        0x8A.toByte() // crc8
    )
    val connection = byteArrayOf(
        0x46, // flag
        7, // idSize
        *"mock_id".toByteArray(), // id
        0b0001_1000 // isConnect and isBanned
    )
    val unknown = byteArrayOf(0x2)
}
