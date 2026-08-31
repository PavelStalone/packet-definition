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

package io.github.pavelstalone.packetdefinition.packet

import io.github.pavelstalone.core.BitBuffer
import io.github.pavelstalone.core.DataSize
import io.github.pavelstalone.core.DataSize.Companion.bits
import io.github.pavelstalone.core.DataSize.Companion.bytes
import io.github.pavelstalone.core.calculateByteSize
import io.github.pavelstalone.packetdefinition.value.Value
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

/**
 * Creates and fills a new packet using the provided [block]
 *
 * @param block The DSL block for filling the packet
 * @return A [ByteBuffer] containing the filled packet data
 */
fun fillPacket(block: FillPacketScope.() -> Unit): ByteBuffer = FillPacketScopeImpl()
    .apply(block)
    .build()

/**
 * DSL scope for filling packet data
 */
interface FillPacketScope {

    /**
     * The total size of the data filled so far
     */
    val size: DataSize

    /**
     * Reserves a specific [size] in the packet
     *
     * @param size The size to reserve
     * @return A [Value] containing Unit
     */
    fun reserve(
        size: DataSize,
    ): Value<Unit>

    /**
     * Adds an integer value to the packet
     *
     * @param value The integer value
     * @param size The size of the integer in the packet
     * @param order The byte order
     * @return A [Value] containing the integer
     */
    fun int(
        value: Int,
        size: DataSize = Int.SIZE_BYTES.bytes,
        order: ByteOrder = ByteOrder.BIG_ENDIAN,
    ): Value<Int>

    /**
     * Adds a float value to the packet
     *
     * @param value The float value
     * @param size The size of the float in the packet
     * @return A [Value] containing the float
     */
    fun float(
        value: Float,
        size: DataSize = Float.SIZE_BYTES.bytes,
    ): Value<Float>

    /**
     * Adds a boolean value to the packet
     *
     * @param value The boolean value
     * @param size The size (number of bits)
     * @return A [Value] containing the boolean
     */
    fun boolean(
        value: Boolean,
        size: DataSize = 1.bits,
    ): Value<Boolean>

    /**
     * Adds a byte value to the packet
     *
     * @param value The byte value
     * @param size The size
     * @return A [Value] containing the byte
     */
    fun byte(
        value: Byte,
        size: DataSize = Byte.SIZE_BYTES.bytes,
    ): Value<Byte>

    /**
     * Adds a byte array to the packet
     *
     * @param value The byte array
     * @param size The size to use in the packet
     * @return A [Value] containing the byte array
     */
    fun bytes(
        value: ByteArray,
        size: DataSize = value.size.bytes,
    ): Value<ByteArray>

    /**
     * Adds a string value to the packet
     *
     * @param value The string value
     * @param charset The character set for encoding
     * @return A [Value] containing the string
     */
    fun string(
        value: String,
        charset: Charset = Charsets.UTF_8,
    ): Value<String>

    /**
     * Adds a custom value to the packet using a [parser] to convert it to bytes
     *
     * @param value The custom value
     * @param size The size of the value in the packet
     * @param parser The function to convert the value to [ByteBuffer]
     * @return A [Value] containing the custom value
     */
    fun <T> custom(
        value: T,
        size: DataSize,
        parser: (value: T) -> ByteBuffer,
    ): Value<T>
}

internal class FillPacketScopeImpl : FillPacketScope {

    private var buffer: BitBuffer = BitBuffer.allocate(0.bytes)

    override val size: DataSize
        get() = buffer.currentSize

    override fun reserve(size: DataSize): Value<Unit> = custom(
        value = Unit,
        size = size
    ) {
        ByteBuffer.allocate(calculateByteSize(size))
    }

    override fun int(
        value: Int,
        size: DataSize,
        order: ByteOrder
    ): Value<Int> = custom(
        value = value,
        size = size
    ) { value ->
        ByteBuffer.allocate(Int.SIZE_BYTES)
            .order(order)
            .putInt(value)
            .rewind()
    }

    override fun float(
        value: Float,
        size: DataSize
    ): Value<Float> = custom(
        value = value,
        size = size
    ) { value ->
        ByteBuffer.allocate(Float.SIZE_BYTES)
            .putFloat(value)
            .rewind()
    }

    override fun boolean(
        value: Boolean,
        size: DataSize
    ): Value<Boolean> = custom(
        value = value,
        size = size
    ) { value ->
        ByteBuffer.wrap(ByteArray(calculateByteSize(size)) { if (value) 0xFF.toByte() else 0x00 })
    }

    override fun byte(
        value: Byte,
        size: DataSize
    ): Value<Byte> = custom(
        value = value,
        size = size
    ) { value ->
        ByteBuffer.allocate(Byte.SIZE_BYTES)
            .put(value)
            .rewind()
    }

    override fun bytes(
        value: ByteArray,
        size: DataSize
    ): Value<ByteArray> = custom(
        value = value,
        size = size
    ) { value ->
        ByteBuffer.wrap(value)
    }

    override fun string(
        value: String,
        charset: Charset
    ): Value<String> {
        val bytes = ByteBuffer.wrap(value.toByteArray(charset = charset))

        return custom(
            value = value,
            size = bytes.capacity().bytes,
        ) { bytes }
    }

    override fun <T> custom(
        value: T,
        size: DataSize,
        parser: (value: T) -> ByteBuffer
    ): Value<T> = Value(
        value = value,
        size = size,
        bytes = parser(value)
    ).also { filledValue ->
        buffer = buffer.expand(filledValue.size)
        buffer.put(bytes = filledValue.bytes.slice(), dataSize = filledValue.size)
    }

    fun build(): ByteBuffer {
        buffer.rewind()
        return buffer.getAll()
    }
}
