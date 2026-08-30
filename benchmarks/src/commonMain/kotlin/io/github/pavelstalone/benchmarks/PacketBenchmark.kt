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

package io.github.pavelstalone.benchmarks

import io.github.pavelstalone.core.BitBuffer
import io.github.pavelstalone.core.DataBuffer
import io.github.pavelstalone.packetdefinition.packet.fillPacket
import io.github.pavelstalone.packetdefinition.packet.packet
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Blackhole
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import java.nio.ByteBuffer

@State(Scope.Benchmark)
open class PacketBenchmark {

    private val bytes1 = byteArrayOf(0, *"Serial".toByteArray(charset = Charsets.UTF_8))
    private val bytes2 = byteArrayOf(26, *ByteBuffer.allocate(4).putInt(83).array())
    private val bytes3 = byteArrayOf(4)

    private val packet = packet {
        val flag = byte().value

        require(flag >= 0) { "Invalid flag" }

        when (flag) {
            4.toByte() -> TestPacket.RegistrationQuery
            26.toByte() -> TestPacket.HeartRate(hr = int().value)

            0.toByte() -> TestPacket.SerialNumber(
                serial = string(
                    size = remainingSize,
                    charset = Charsets.UTF_8
                ).value
            )

            else -> error("Not supported flag")
        }
    }

    @Benchmark
    fun parsePacketsWithWholeBytes(bh: Blackhole) {
        bh.consume(packet.fill(bytes1))
        bh.consume(packet.fill(bytes2))
        bh.consume(packet.fill(bytes3))
    }

    @Benchmark
    fun lowLevelParsePacketsWithWholeBytes(bh: Blackhole) {
        bh.consume(parse(bytes1))
        bh.consume(parse(bytes2))
        bh.consume(parse(bytes3))
    }

    @Benchmark
    fun fillPacketsWithWholeBytes(bh: Blackhole) {
        bh.consume(
            fillPacket {
                byte(0)
                string("Serial")
            }
        )
        bh.consume(
            fillPacket {
                byte(26)
                int(83)
            }
        )
        bh.consume(
            fillPacket {
                byte(4)
            }
        )
    }

    @Benchmark
    fun lowLevelFillPacketsWithWholeBytesUseByteBuffer(bh: Blackhole) {
        bh.consume(fillFirstPacketByByteBuffer())
        bh.consume(fillSecondPacketByByteBuffer())
        bh.consume(fillThirdPacketByByteBuffer())
    }

    @Benchmark
    fun lowLevelFillPacketsWithWholeBytesUseDataBuffer(bh: Blackhole) {
        bh.consume(fillFirstPacketByBitBuffer())
        bh.consume(fillSecondPacketByBitBuffer())
        bh.consume(fillThirdPacketByBitBuffer())
    }

    private fun fillFirstPacketByByteBuffer(): ByteBuffer {
        val serial = "Serial".toByteArray(charset = Charsets.UTF_8)
        return ByteBuffer.allocate(serial.size + 1)
            .put(0)
            .put(serial)
            .rewind()
    }

    private fun fillSecondPacketByByteBuffer(): ByteBuffer {
        return ByteBuffer.allocate(5)
            .put(26)
            .putInt(83)
            .rewind()
    }

    private fun fillThirdPacketByByteBuffer(): ByteBuffer {
        return ByteBuffer.allocate(1)
            .put(4)
            .rewind()
    }

    private fun fillFirstPacketByBitBuffer(): DataBuffer {
        val serial = "Serial".toByteArray(charset = Charsets.UTF_8)
        return BitBuffer.wrap(ByteBuffer.allocate(serial.size + 1))
            .put(0.toByte())
            .putAll(serial)
            .rewind()
    }

    private fun fillSecondPacketByBitBuffer(): DataBuffer {
        return BitBuffer.wrap(ByteBuffer.allocate(5))
            .put(26.toByte())
            .put(83)
            .rewind()
    }

    private fun fillThirdPacketByBitBuffer(): DataBuffer {
        return BitBuffer.wrap(ByteBuffer.allocate(1))
            .put(4.toByte())
            .rewind()
    }

    private fun parse(byteArray: ByteArray): TestPacket {
        val buf = ByteBuffer.wrap(byteArray)

        val flag = buf.get()

        return when (flag) {
            4.toByte() -> TestPacket.RegistrationQuery
            26.toByte() -> TestPacket.HeartRate(hr = buf.getInt())
            0.toByte() -> {
                val arr = ByteArray(buf.remaining())
                buf.get(arr)
                TestPacket.SerialNumber(serial = arr.toString(Charsets.UTF_8))
            }

            else -> error("Not supported flag")
        }
    }
}

private sealed interface TestPacket {

    data object RegistrationQuery : TestPacket

    data class HeartRate(val hr: Int) : TestPacket
    data class SerialNumber(val serial: String) : TestPacket
}
