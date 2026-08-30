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

package io.github.pavelstalone.core

import io.github.pavelstalone.core.DataSize.Companion.bits
import io.github.pavelstalone.core.DataSize.Companion.bytes
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Interface for reading and writing data with bit-level precision
 */
interface DataBuffer {

    /**
     * The amount of data remaining in the buffer
     */
    val remainingSize: DataSize

    /**
     * The amount of data already processed (read or written) in the buffer
     */
    val currentSize: DataSize

    /**
     * Resets the buffer's position and bit offset to zero
     *
     * @return This buffer instance
     */
    fun rewind(): DataBuffer

    /**
     * Reads a specified amount of data from the current position
     *
     * @param size The amount of data to read
     * @return A [ByteBuffer] containing the read data
     */
    fun get(size: DataSize): ByteBuffer

    /**
     * Reads all remaining data from the current position to the end of the buffer
     *
     * @return A [ByteBuffer] containing the remaining data
     */
    fun getAll(): ByteBuffer

    /**
     * Advances the current position by the specified size without reading or writing data
     *
     * @param dataSize The amount of data to skip
     * @return This buffer instance
     */
    fun reserve(dataSize: DataSize): DataBuffer

    /**
     * Writes a byte array into the buffer at the current position
     *
     * @param bytes The data to write
     * @param dataSize The number of bits to write from the byte array
     * @return This buffer instance
     */
    fun put(bytes: ByteArray, dataSize: DataSize): DataBuffer

    /**
     * Writes a [ByteBuffer] into the buffer at the current position
     *
     * @param bytes The data to write
     * @param dataSize The number of bits to write from the [ByteBuffer]
     * @return This buffer instance
     */
    fun put(bytes: ByteBuffer, dataSize: DataSize): DataBuffer

    /**
     * Writes a boolean value
     *
     * @param value The boolean value to write
     * @param dataSize The number of bits to use for this value
     * @return This buffer instance
     */
    fun put(value: Boolean, dataSize: DataSize = 1.bits): DataBuffer

    /**
     * Writes a byte value
     *
     * @param value The byte value to write
     * @param dataSize The number of bits to use for this value
     * @return This buffer instance
     */
    fun put(value: Byte, dataSize: DataSize = Byte.SIZE_BYTES.bytes): DataBuffer

    /**
     * Writes an integer value
     *
     * @param value The integer value to write
     * @param dataSize The number of bits to use for this value
     * @return This buffer instance
     */
    fun put(value: Int, dataSize: DataSize = Int.SIZE_BYTES.bytes): DataBuffer

    /**
     * Writes a long value
     *
     * @param value The long value to write
     * @param dataSize The number of bits to use for this value
     * @return This buffer instance
     */
    fun put(value: Long, dataSize: DataSize = Long.SIZE_BYTES.bytes): DataBuffer

    /**
     * Writes the entire byte array into the buffer
     *
     * @param bytes The byte array to write
     * @return This buffer instance
     */
    fun putAll(bytes: ByteArray): DataBuffer

    /**
     * Writes all remaining data from the given [ByteBuffer] into this buffer
     *
     * @param bytes The [ByteBuffer] containing data to write
     * @return This buffer instance
     */
    fun putAll(bytes: ByteBuffer): DataBuffer
}

/**
 * An implementation of [DataBuffer] that uses a [ByteBuffer] as the underlying storage
 */
class BitBuffer internal constructor(
    private val buffer: ByteBuffer
) : DataBuffer {

    private var bitPosition: Int = 0

    override val remainingSize: DataSize
        get() = buffer.remaining().bytes - bitPosition.bits
    override val currentSize: DataSize
        get() = buffer.position().bytes + bitPosition.bits

    override fun rewind(): DataBuffer {
        bitPosition = 0
        buffer.position(0)

        return this
    }

    override fun get(size: DataSize): ByteBuffer {
        // Optimized case for retrieving whole bytes
        if (size.bitSize % 8 == 0 && bitPosition == 0) return getFast(size)

        return getWithShift(size)
    }

    override fun getAll(): ByteBuffer = buffer.slice().also {
        buffer.position(buffer.limit())
        bitPosition = 0
    }

    override fun reserve(dataSize: DataSize): DataBuffer {
        val newBitPosition = (dataSize.bitSize + bitPosition)
        val byteShiftPosition = newBitPosition / Byte.SIZE_BITS

        buffer.position(buffer.position() + byteShiftPosition)
        bitPosition = newBitPosition % Byte.SIZE_BITS

        return this
    }

    override fun put(bytes: ByteArray, dataSize: DataSize): DataBuffer =
        put(bytes = ByteBuffer.wrap(bytes), dataSize = dataSize)

    override fun put(
        bytes: ByteBuffer,
        dataSize: DataSize
    ): DataBuffer {
        if (dataSize.bitSize <= 0) return this

        // Optimized case for putting whole bytes
        if (bitPosition == 0 && dataSize.bitSize % 8 == 0) putFast(bytes, dataSize)
        else putWithShift(bytes, dataSize)

        return this
    }

    override fun put(value: Boolean, dataSize: DataSize): DataBuffer {
        val size = calculateByteSize(dataSize)

        return put(ByteArray(size) { if (value) 0xFF.toByte() else 0x00 }, dataSize)
    }

    override fun put(value: Byte, dataSize: DataSize): DataBuffer {
        require(dataSize.bitSize <= Byte.SIZE_BITS)

        if (bitPosition == 0 && dataSize.bitSize == Byte.SIZE_BITS) buffer.put(value)
        else putByte(value, dataSize)

        return this
    }

    override fun put(value: Int, dataSize: DataSize): DataBuffer {
        require(dataSize.bitSize <= Int.SIZE_BITS)
        if (bitPosition == 0 && dataSize.bitSize == Int.SIZE_BITS) {
            buffer.putInt(value)
        } else {
            val size = calculateByteSize(dataSize)

            if (size == Int.SIZE_BYTES) putInt(value, dataSize)
            else put(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(value).rewind(), dataSize)
        }

        return this
    }

    override fun put(value: Long, dataSize: DataSize): DataBuffer {
        require(dataSize.bitSize <= Long.SIZE_BITS)

        if (bitPosition == 0 && dataSize.bitSize == Long.SIZE_BITS) {
            buffer.putLong(value)
        } else {
            val size = calculateByteSize(dataSize)

            if (size == Long.SIZE_BYTES) putLong(value, dataSize)
            else put(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(value).rewind(), dataSize)
        }

        return this
    }

    override fun putAll(bytes: ByteArray): DataBuffer {
        if (bitPosition == 0) buffer.put(bytes)
        else putWithShift(bytes = ByteBuffer.wrap(bytes), dataSize = bytes.size.bytes)

        return this
    }

    override fun putAll(bytes: ByteBuffer): DataBuffer {
        if (bitPosition == 0) buffer.put(bytes)
        else putWithShift(bytes = bytes, dataSize = bytes.remaining().bytes)

        return this
    }

    fun expand(size: DataSize): BitBuffer {
        val neededSize = size.bitSize - remainingSize.bitSize

        if (neededSize <= 0) return this
        return BitBuffer(
            ByteBuffer.allocate(buffer.capacity() + calculateByteSize(neededSize.bits))
                .put(buffer.duplicate().rewind())
        ).also { newBuffer ->
            newBuffer.buffer.position(buffer.position())
            newBuffer.bitPosition = bitPosition
        }
    }

    private fun getFast(size: DataSize): ByteBuffer {
        val bytesCount = size.bitSize / 8

        return buffer.slice().limit(bytesCount).also {
            buffer.position(buffer.position() + bytesCount)
        }
    }

    private fun getWithShift(size: DataSize): ByteBuffer {
        val bitOffset = size.bitSize % 8
        // How much the result will be shifted
        val resultRightShift = if (bitOffset == 0) 0 else 8 - bitOffset
        // How much the buffer is already shifted
        val bufferRightShifted = bitPosition
        // Direction and amount the buffer needs to be moved
        val overlapRightShift = resultRightShift - bufferRightShifted
        // Total bytes in the result
        val resultBytesSize = calculateByteSize(size)
        // Total bytes to take from the buffer
        val bufferBytesSize = ceil((size.bitSize + bitPosition) / 8.0).toInt()

        // Slice from the common buffer to fill the result
        val bufferBytes = buffer.slice().limit(bufferBytesSize)
        // Buffer where the result will gradually appear
        val resultBytes = ByteBuffer.allocate(resultBytesSize)

        while (resultBytes.hasRemaining() && bufferBytes.hasRemaining()) {
            if (overlapRightShift > 0) { // Shift everything to the right
                when {
                    // Use Long for shifting
                    bufferBytes.remaining() >= Long.SIZE_BYTES && resultBytes.remaining() >= Long.SIZE_BYTES -> {
                        var num = bufferBytes.getLong()
                        // Mask to keep only shifted bits
                        val longShiftedMask = -1L ushr overlapRightShift

                        num = num shr overlapRightShift and longShiftedMask
                        val leftNumPos = bufferBytes.position() - Long.SIZE_BYTES - 1
                        // If there is a number to the left, take its tail
                        if (leftNumPos >= 0) {
                            // Mask to keep only the required tail
                            val tailMask = (1L shl overlapRightShift) - 1
                            val leftTail = bufferBytes.get(leftNumPos).toLong() and tailMask

                            num = leftTail shl (Long.SIZE_BITS - overlapRightShift) or num
                        } else { // If no number to the left, we are at the head
                            val tailMask = -1L ushr resultRightShift
                            num = num and tailMask
                            // Preserve negative sign if present
                            num = num shl resultRightShift shr resultRightShift
                        }

                        resultBytes.putLong(num)
                    }

                    // Use Int for shifting
                    bufferBytes.remaining() >= Int.SIZE_BYTES && resultBytes.remaining() >= Int.SIZE_BYTES -> {
                        var num = bufferBytes.getInt()
                        // Mask to keep only shifted bits
                        val intShiftedMask = -1 ushr overlapRightShift

                        num = num shr overlapRightShift and intShiftedMask
                        val leftNumPos = bufferBytes.position() - Int.SIZE_BYTES - 1
                        // If there is a number to the left, take its tail
                        if (leftNumPos >= 0) {
                            // Mask to keep only the required tail
                            val tailMask = (1 shl overlapRightShift) - 1
                            val leftTail = bufferBytes.get(leftNumPos).toInt() and tailMask

                            num = leftTail shl (Int.SIZE_BITS - overlapRightShift) or num
                        } else { // If no number to the left, we are at the head
                            val tailMask = -1 ushr resultRightShift
                            num = num and tailMask
                            // Preserve negative sign if present
                            num = num shl resultRightShift shr resultRightShift
                        }

                        resultBytes.putInt(num)
                    }

                    // Use Byte for shifting
                    else -> {
                        var num = bufferBytes.get().toUByte().toInt()
                        // Mask to keep only shifted bits

                        val byteShiftedMask = 0xFF ushr overlapRightShift

                        num = num shr overlapRightShift and byteShiftedMask
                        val leftNumPos = bufferBytes.position() - Byte.SIZE_BYTES - 1
                        // If there is a number to the left, take its tail
                        if (leftNumPos >= 0) {
                            // Mask to keep only the required tail
                            val tailMask = (1 shl overlapRightShift) - 1
                            val leftTail = bufferBytes.get(leftNumPos).toInt() and tailMask

                            num = leftTail shl (Byte.SIZE_BITS - overlapRightShift) or num
                        } else { // If no number to the left, we are at the head
                            val tailMask = 0xFF ushr resultRightShift
                            num = num and tailMask
                            // Preserve negative sign if present
                            num = (num shl resultRightShift).toByte().toInt() shr resultRightShift
                        }

                        resultBytes.put(num.toByte())
                    }
                }
            } else if (overlapRightShift < 0) { // Shift everything to the left
                val overlapLeftShift = abs(overlapRightShift)

                when {
                    // Use Long for shifting
                    bufferBytes.remaining() >= Long.SIZE_BYTES -> {
                        var num = bufferBytes.getLong()
                        // Mask to keep only shifted bits
                        val longShiftedMask = -1L shl overlapLeftShift

                        num = num shl overlapLeftShift and longShiftedMask
                        // If there is a number to the right, take its head
                        if (bufferBytes.hasRemaining()) {
                            val rightHead = bufferBytes.get(bufferBytes.position()).toUByte()
                                .toLong() ushr (Byte.SIZE_BITS - overlapLeftShift)

                            num = num or rightHead
                        }

                        resultBytes.putLong(num)
                    }

                    // Use Int for shifting
                    bufferBytes.remaining() >= Int.SIZE_BYTES -> {
                        var num = bufferBytes.getInt()
                        // Mask to keep only shifted bits
                        val intShiftedMask = -1 shl overlapLeftShift

                        num = num shl overlapLeftShift and intShiftedMask
                        // If there is a number to the right, take its head
                        if (bufferBytes.hasRemaining()) {
                            val rightHead = bufferBytes.get(bufferBytes.position()).toUByte()
                                .toInt() ushr (Byte.SIZE_BITS - overlapLeftShift)

                            num = num or rightHead
                        }

                        resultBytes.putInt(num)
                    }

                    // Use Byte for shifting
                    else -> {
                        var num = bufferBytes.get().toUByte().toInt()
                        // Mask to keep only shifted bits
                        val byteShiftedMask = -1 shl overlapLeftShift

                        num = num shl overlapLeftShift and byteShiftedMask
                        // If there is a number to the right, take its head
                        if (bufferBytes.hasRemaining()) {
                            val rightHead = bufferBytes.get(bufferBytes.position()).toUByte()
                                .toInt() ushr (Byte.SIZE_BITS - overlapLeftShift)

                            num = num or rightHead
                        }

                        resultBytes.put(num.toByte())
                    }
                }
            } else { // No shifting needed
                resultBytes.put(bufferBytes)
                resultBytes.rewind()

                val headMask = 0xFF ushr bufferRightShifted
                val head = resultBytes.get(0).toInt() and headMask
                val num = (head shl resultRightShift).toByte().toInt() shr resultRightShift

                resultBytes.put(num.toByte())
            }
        }

        bitPosition = (bufferRightShifted + size.bitSize) % 8
        buffer.position(buffer.position() + (bufferRightShifted + size.bitSize) / 8)

        return resultBytes.rewind()
    }

    private fun putFast(bytes: ByteBuffer, dataSize: DataSize) {
        val bytesCount = dataSize.bitSize / 8
        val source = bytes.apply { position(remaining() - bytesCount) }

        buffer.put(source)
    }

    private fun putWithShift(bytes: ByteBuffer, dataSize: DataSize) {
        var valueSize = dataSize
        val countValueBytes = bytes.remaining()

        while (valueSize.bitSize > 0) {
            val byteSize = calculateByteSize(valueSize)
            val startBytePos = countValueBytes - byteSize

            val valueBits = when {
                byteSize >= Long.SIZE_BYTES -> putLong(bytes.getLong(startBytePos), valueSize)
                byteSize >= Int.SIZE_BYTES -> putInt(bytes.getInt(startBytePos), valueSize)
                else -> putByte(bytes.get(startBytePos), valueSize)
            }

            valueSize -= valueBits.bits
        }
    }

    private fun putLong(value: Long, dataSize: DataSize): Int {
        val countValueBits = dataSize.bitSize % Long.SIZE_BITS

        val valueBits = if (countValueBits == 0) Long.SIZE_BITS else countValueBits
        val valueShift = Long.SIZE_BITS - valueBits

        val remainingBits = Long.SIZE_BITS - bitPosition
        val bitDiff = remainingBits - valueBits

        val shift = bitPosition - valueShift
        val bufferValue = buffer.getLong(buffer.position())
        val tailMask = if (valueBits == Long.SIZE_BITS) -1L else (0x1L shl valueBits) - 1
        val tailValue = value and tailMask

        val newValue = when {
            shift > 0 -> {
                bufferValue or (tailValue ushr shift)
            }

            shift < 0 -> {
                bufferValue or (tailValue shl abs(shift))
            }

            else -> bufferValue or tailValue
        }

        when {
            bitDiff > 0 -> {
                bitPosition += valueBits
                buffer.putLong(buffer.position(), newValue)
            }

            bitDiff < 0 -> {
                val diff = abs(bitDiff)

                bitPosition = diff % Byte.SIZE_BITS
                buffer.putLong(newValue)
                buffer.put(
                    buffer.position(),
                    (tailValue shl (valueShift + remainingBits - Long.SIZE_BITS + Byte.SIZE_BITS)).toByte()
                )
            }

            else -> {
                bitPosition = 0
                buffer.putLong(newValue)
            }
        }

        return valueBits
    }

    private fun putInt(value: Int, dataSize: DataSize): Int {
        val countValueBits = dataSize.bitSize % Int.SIZE_BITS

        val valueBits = if (countValueBits == 0) Int.SIZE_BITS else countValueBits
        val valueShift = Int.SIZE_BITS - valueBits

        val remainingBits = Int.SIZE_BITS - bitPosition
        val bitDiff = remainingBits - valueBits

        val shift = bitPosition - valueShift
        val bufferValue = buffer.getInt(buffer.position())
        val tailMask = if (valueBits == Int.SIZE_BITS) -1 else (0x1 shl valueBits) - 1
        val tailValue = value and tailMask

        val newValue = when {
            shift > 0 -> {
                bufferValue or (tailValue ushr shift)
            }

            shift < 0 -> {
                bufferValue or (tailValue shl abs(shift))
            }

            else -> bufferValue or tailValue
        }

        when {
            bitDiff > 0 -> {
                bitPosition += valueBits
                buffer.putInt(buffer.position(), newValue)
            }

            bitDiff < 0 -> {
                val diff = abs(bitDiff)

                bitPosition = diff % Byte.SIZE_BITS
                buffer.putInt(newValue)
                buffer.put(
                    buffer.position(),
                    (tailValue shl (valueShift + remainingBits - Int.SIZE_BITS + Byte.SIZE_BITS)).toByte()
                )
            }

            else -> {
                bitPosition = 0
                buffer.putInt(newValue)
            }
        }

        return valueBits
    }

    private fun putByte(value: Byte, dataSize: DataSize): Int {
        val countValueBits = dataSize.bitSize % Byte.SIZE_BITS

        val valueBits = if (countValueBits == 0) Byte.SIZE_BITS else countValueBits
        val valueShift = Byte.SIZE_BITS - valueBits

        val remainingBits = Byte.SIZE_BITS - bitPosition
        val bitDiff = remainingBits - valueBits

        val shift = bitPosition - valueShift
        val bufferValue = buffer.get(buffer.position()).toUByte().toInt()
        val tailMask = (0x1 shl valueBits) - 1
        val tailValue = value.toUByte().toInt() and tailMask

        val newValue = when {
            shift > 0 -> {
                bufferValue or (tailValue ushr shift)
            }

            shift < 0 -> {
                bufferValue or (tailValue shl abs(shift))
            }

            else -> bufferValue or tailValue
        }

        when {
            bitDiff > 0 -> {
                bitPosition += valueBits
                buffer.put(buffer.position(), newValue.toByte())
            }

            bitDiff < 0 -> {
                bitPosition = abs(bitDiff)
                buffer.put(newValue.toByte())
                buffer.put(
                    buffer.position(),
                    (tailValue shl (valueShift + remainingBits)).toByte()
                )
            }

            else -> {
                bitPosition = 0
                buffer.put(newValue.toByte())
            }
        }

        return valueBits
    }

    companion object {

        /**
         * Wraps a byte array into a [BitBuffer]
         *
         * @param byteArray The byte array to wrap
         * @return A new [BitBuffer] instance
         */
        fun wrap(byteArray: ByteArray): BitBuffer = BitBuffer(ByteBuffer.wrap(byteArray))

        /**
         * Wraps a [ByteBuffer] into a [BitBuffer]
         *
         * @param byteBuffer The byte buffer to wrap
         * @return A new [BitBuffer] instance
         */
        fun wrap(byteBuffer: ByteBuffer): BitBuffer = BitBuffer(byteBuffer.slice())

        /**
         * Wraps a [ByteBuffer] into a [BitBuffer] with a specific size and bit-level offset
         *
         * @param byteBuffer The byte buffer to wrap
         * @param dataSize The size of the data in bits or bytes
         * @param lsb Specifies where to take the information from: the end of the ByteBuffer (true, LSB) or the beginning (false, MSB). This is important when the data size is not a multiple of 8 bits
         * @return A new [BitBuffer] instance
         */
        fun wrap(byteBuffer: ByteBuffer, dataSize: DataSize, lsb: Boolean = true): BitBuffer =
            BitBuffer(
                byteBuffer.slice().apply {
                    if (lsb) position(remaining() - calculateByteSize(dataSize))
                }
            ).apply {
                if (lsb) {
                    val bits = (dataSize.bitSize % Byte.SIZE_BITS)
                    bitPosition = if (bits == 0) 0 else Byte.SIZE_BITS - bits
                }
            }

        fun allocate(size: DataSize): BitBuffer {
            val size = if (size.bitSize % 8 == 0) size.bitSize / 8 else calculateByteSize(size)

            return BitBuffer(ByteBuffer.allocate(size))
        }
    }
}

/**
 * Calculates the number of full bytes required to store the given [dataSize]
 *
 * @param dataSize The size in bits or bytes
 * @return The number of bytes needed (rounded up)
 */
fun calculateByteSize(dataSize: DataSize): Int =
    ceil(dataSize.bitSize / Byte.SIZE_BITS.toFloat()).toInt()

/**
 * Iterates over the remaining bytes in this [ByteBuffer] and applies the [action] to each byte
 *
 * @param action The function to be applied to each byte
 */
inline fun ByteBuffer.forEach(action: (Byte) -> Unit) {
    while (hasRemaining()) {
        action(get())
    }
}
