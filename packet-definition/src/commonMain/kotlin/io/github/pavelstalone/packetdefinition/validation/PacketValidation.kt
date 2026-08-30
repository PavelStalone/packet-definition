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

package io.github.pavelstalone.packetdefinition.validation

import io.github.pavelstalone.packetdefinition.value.Value

/**
 * Interface for packet data validation
 *
 * Implementations can provide different validation algorithms
 * to verify packet integrity or correctness
 */
fun interface PacketValidation {

    /**
     * Validates a sequence of filled values from a packet
     *
     * The filled values are provided in the exact same order as they are defined
     * in the packet structure, allowing implementations to process fields in their
     * correct sequence
     *
     * @param values list of filled values from the packet to validate, in structural order
     * @return True if validation passes, false otherwise
     */
    fun validate(vararg values: Value<*>): Boolean
}
