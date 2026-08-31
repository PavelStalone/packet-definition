# packet-definition

[![Kotlin Alpha](https://kotl.in/badges/alpha.svg)](https://kotlinlang.org/docs/components-stability.html) [![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE) ![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-blue.svg?logo=kotlin)

![JVM](https://img.shields.io/badge/platform-JVM-orange.svg) ![Android](https://img.shields.io/badge/platform-Android-brightgreen.svg?logo=android)

**PacketDefinition** is a declarative, type-safe DSL for defining and working with binary data packets in Kotlin Multiplatform projects. 

Working with binary protocols (such as BLE (Bluetooth Low Energy), custom radio protocols, or hardware sensor data) often involves tedious and error-prone manual byte manipulation, mask operations, and offset tracking. This library abstracts away these complexities, allowing you to describe your packet structure in a clean, readable way.

### Key Features
*   **Bit-Level Precision**: Work with fields of any size (e.g., a 4-bit integer or a 1-bit boolean).
*   **Declarative DSL**: Describe "what" the packet looks like, not "how" to parse it.
*   **Automatic Sign Extension**: Correctly handles signed values even in arbitrary bit sizes.
*   **Composable & Nested**: Easily reuse packet definitions and logic through nesting.
*   **Built-in Validation**: Simple API for integrating CRC checks and data requirements.

---

## Core Concepts

### BitBuffer
At the heart of the library is `BitBuffer`. While standard programming environments typically treat 8 bits (1 byte) as the smallest addressable unit, `BitBuffer` allows you to navigate and extract data at the bit level.

It maintains a bit-level position and handles all the necessary bitwise operations (shifting and masking) under the hood. When you request 7 bits, `BitBuffer` extracts exactly those 7 bits from the current stream, even if they span across two different bytes.

### Bit Representation & Sign Extension
When working with bits, representing negative numbers can be tricky. PacketDefinition handles this by applying **Sign Extension**.

For example, if you have a **3-bit signed integer** with the value `100` (binary):
- In a 3-bit context, this represents `-4`.
- When converted to a standard Kotlin `Int` (32-bit), the library ensures it remains `-4` by representing it as `1111...1100` (hex `0xFFFF FF0C` or similar depending on size). 
This ensures that your logic remains consistent regardless of the underlying bit-width of the protocol field.

---

## Modules

The library is split into two modules to separate low-level buffer operations from high-level DSL:

*   `packet-definition-core`: Provides `BitBuffer`, and `DataSize`. Use this for low-level manual data manipulation.
*   `packet-definition`: Provides the `packet { ... }` and `fillPacket { ... }` DSLs for a high-level, declarative approach.

---

## Using in your projects

> Note that the library is experimental, and the API is subject to change.

### Gradle (Kotlin DSL)

Make sure that you have `mavenCentral()` in the list of repositories:
```kotlin
repositories {
    mavenCentral()
}
```

Add the library to dependencies:
```kotlin
dependencies {
    implementation("io.github.pavelstalone:packet-definition:0.0.0")
}
```

For Kotlin Multiplatform:
```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("io.github.pavelstalone:packet-definition:0.0.0")
            }
        }
    }
}
```

---

## Usage Examples

### Defining and Parsing a Packet
You can define a packet once and use it to parse data from `ByteArray` or `ByteBuffer`.

```kotlin
val myPacket = packet {
    val version = int(4.bits).value    // Reads 4 bits as an Int
    val flag = boolean().value         // Reads 1 bit as a Boolean
    val type = int(3.bits).value       // Reads 3 bits as an Int
    val payloadSize = int(1.bytes).value
    val payload = string(payloadSize.bytes).value

    "Packet v$version, Type: $type, Data: $payload"
}

// Filling the packet
val result: String = myPacket.fill(byteArrayOf(0xB2.toByte(), 0x04.toByte(), 0x48, 0x69))
```

### Bit-level Fields & Validation
The library makes it easy to add validation logic directly into the parsing scope.

```kotlin
data class SensorData(val value: Float, val isActive: Boolean)

val sensorPacket = packet {
    val isActive = boolean() // Returns Value<Boolean>
    val reserved = reserve(7.bits)
    val value = float(4.bytes) // Returns Value<Float>
    
    val crc = byte().value
    // Validate using the Value objects directly
    require(CRC8Validation(crc).validate(isActive, value)) { "Invalid CRC!" }
    
    SensorData(value.value, isActive.value)
}
```

### Nested Structures
You can compose packets using `from`.

```kotlin
val connectionPacket = packet {
    val idSize = int(1.bytes).value
    val id = string(idSize.bytes).value
    
    // Using 'from' to parse a sub-structure from the current remaining bytes
    val details = from(bytes(remainingSize)) {
        val port = int(2.bytes).value
        val isSecure = boolean().value
        port to isSecure
    }
    
    id to details
}
```

Or calling a pre-defined parsing function (Composition).

```kotlin
val mainDefinition = packet {
    val header = int(1.bytes).value
    
    when(header) {
        0x01 -> sensorType()
        else -> "Unknown"
    }
}

fun PacketScope.sensorType(): String {
    val type = int(1.bytes).value
    
    return when(type) {
        0x2F -> "PulseOx"
        else -> "Unknown"
    }
}
```

### Constructing Packets (Filling)
To create raw binary data, use `fillPacket`. It allows you to specify values and their bit-sizes.

```kotlin
val buffer: ByteBuffer = fillPacket {
    val version = int(value = 1, size = 4.bits)
    val flag = boolean(value = true)
    val type = int(value = 2, size = 3.bits)
    
    val message = "Hello"
    int(value = message.length, size = 1.bytes)
    string(value = message)
    
    val crc = CRC8Validation.calculate(version, flag, type)
    byte(crc)
}
```

## License

    Copyright 2026 Pavel Shoplik
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

---

I hope this library makes your life a little easier when dealing with binary protocols! **May the Force be with you!**