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
import io.github.pavelstalone.core.DataBuffer
import io.github.pavelstalone.core.DataSize
import io.github.pavelstalone.core.DataSize.Companion.bits
import io.github.pavelstalone.core.DataSize.Companion.bytes
import io.github.pavelstalone.packetdefinition.value.Value
import io.github.pavelstalone.packetdefinition.value.ValueParser
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

/**
 * DSL marker for packet builder scope
 */
@DslMarker
annotation class PacketScopeMarker

/**
 * Creates a new [Packet] definition using the provided [block]
 *
 * @param T The type returned by the packet initializer
 * @param block The DSL block defining the packet structure
 * @return A [Packet] instance
 */
fun <T> packet(
    block: PacketScope.() -> T,
): Packet<T> = Packet(initializer = block)

/**
 * Represents a packet definition that can be filled from various data sources
 *
 * @param T The type returned when the packet is filled
 */
class Packet<T> internal constructor(
    private val initializer: PacketScope.() -> T,
) {

    /**
     * Fills the packet from a [ByteArray]
     *
     * @param byteArray The data source
     * @return The result of the packet initializer
     */
    fun fill(byteArray: ByteArray): T {
        val scope = PacketScopeImpl(dataBuffer = BitBuffer.wrap(byteArray))
        return scope.initializer()
    }

    /**
     * Fills the packet from a [ByteBuffer]
     *
     * @param byteBuffer The data source
     * @return The result of the packet initializer
     */
    fun fill(byteBuffer: ByteBuffer): T {
        val scope = PacketScopeImpl(dataBuffer = BitBuffer.wrap(byteBuffer))
        return scope.initializer()
    }

    /**
     * Fills the packet from a [DataBuffer]
     *
     * @param dataBuffer The data source
     * @return The result of the packet initializer
     */
    fun fill(dataBuffer: DataBuffer): T {
        val scope = PacketScopeImpl(dataBuffer = dataBuffer)
        return scope.initializer()
    }
}

/**
 * DSL scope for defining packet fields
 */
@PacketScopeMarker
interface PacketScope {

    /**
     * Remaining size in the current packet
     */
    val remainingSize: DataSize

    /**
     * Reserves a specific [size] without parsing it
     *
     * @param size The size to reserve
     * @return A [Value] containing Unit
     */
    fun reserve(size: DataSize): Value<Unit> = custom(
        size = size,
        valueParser = {},
    )

    /**
     * Parses an integer of a specific [size]
     *
     * @param size The size in bits or bytes
     * @param byteOrder The byte order for parsing
     * @return A [Value] containing the parsed Int
     */
    fun int(
        size: DataSize = Int.SIZE_BYTES.bytes,
        byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN,
    ): Value<Int> = custom(
        size = size,
        valueParser = { buffer ->
            buffer.order(byteOrder)
            ValueParser.IntParser.parse(buffer)
        },
    )

    /**
     * Parses a float of a specific [size]
     *
     * @param size The size in bits or bytes
     * @return A [Value] containing the parsed Float
     */
    fun float(size: DataSize = 2.bytes): Value<Float> = custom(
        size = size,
        valueParser = ValueParser.FloatParser,
    )

    /**
     * Parses a boolean value
     *
     * @param size The size in bits
     * @return A [Value] containing the parsed Boolean
     */
    fun boolean(size: DataSize = 1.bits): Value<Boolean> = custom(
        size = size,
        valueParser = ValueParser.BooleanParser,
    )

    /**
     * Parses a single byte
     *
     * @param size The size (usually 1 byte)
     * @return A [Value] containing the parsed Byte
     */
    fun byte(size: DataSize = 1.bytes): Value<Byte> = custom(
        size = size,
        valueParser = { buffer -> buffer.get() },
    )

    /**
     * Parses a sequence of bytes
     *
     * @param size The number of bits or bytes to read
     * @return A [Value] containing the ByteArray
     */
    fun bytes(size: DataSize): Value<ByteArray> = custom(
        size = size,
        valueParser = { buffer ->
            ByteArray(buffer.remaining()).also { buffer.get(it) }
        },
    )

    /**
     * Parses a string of a specific [size]
     *
     * @param size The size in bits or bytes
     * @param charset The character set to use for decoding
     * @return A [Value] containing the decoded String
     */
    fun string(
        size: DataSize,
        charset: Charset = Charsets.UTF_8,
    ): Value<String> = custom(
        size = size,
        valueParser = { buffer ->
            if (buffer.hasArray()) {
                String(
                    bytes = buffer.array(),
                    offset = buffer.arrayOffset(),
                    length = buffer.remaining(),
                    charset = charset,
                )
            } else {
                charset.decode(buffer).toString()
            }
        },
    )

    /**
     * Parses a custom value using a [valueParser]
     *
     * @param size The size of the data to parse
     * @param valueParser The parser implementation
     * @return A [Value] containing the parsed object
     */
    fun <T> custom(size: DataSize, valueParser: ValueParser<T>): Value<T>

    /**
     * Enters a nested scope using the content of a [Value]
     *
     * @param value The value to use as a data source for the nested scope
     * @param block The DSL block for the nested scope
     * @return The result of the nested block
     */
    fun <T> from(value: Value<*>, block: PacketScope.() -> T): T

    /**
     * Fills a nested [packet] using the content of a [Value]
     *
     * @param value The value to use as a data source
     * @param packet The packet definition to fill
     * @return The result of filling the nested packet
     */
    fun <T> from(value: Value<*>, packet: Packet<T>): T
}

private class PacketScopeImpl(
    private val dataBuffer: DataBuffer,
) : PacketScope {

    override val remainingSize: DataSize
        get() = dataBuffer.remainingSize

    override fun <T> custom(
        size: DataSize,
        valueParser: ValueParser<T>
    ): Value<T> {
        val array = dataBuffer.get(size)
        val value = valueParser.parse(array)

        return Value(
            size = size,
            bytes = array.rewind(),
            value = value,
        )
    }

    override fun <T> from(
        value: Value<*>,
        block: PacketScope.() -> T
    ): T {
        val scope = PacketScopeImpl(BitBuffer.wrap(value.bytes, value.size))
        return scope.block()
    }

    override fun <T> from(value: Value<*>, packet: Packet<T>): T {
        return packet.fill(dataBuffer = BitBuffer.wrap(value.bytes, value.size))
    }
}
