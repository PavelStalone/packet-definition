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

package io.github.pavelstalone.packetdefinition.value

import io.github.pavelstalone.core.DataSize
import java.nio.ByteBuffer

/**
 * Represents a value from a packet
 *
 * @param T The type of the value
 * @property value The actual value
 * @property size The size of the data used for this value
 * @property bytes The raw bytes that represent this value
 */
data class Value<T>(
    val value: T,
    val size: DataSize,
    val bytes: ByteBuffer,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Value<*>

        if (value != other.value) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value?.hashCode() ?: 0
        result = 31 * result + size.hashCode()
        return result
    }
}
