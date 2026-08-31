plugins {
    id("packet-definition-multiplatform")
    id("packet-definition-publish")
}

artifact {
    id = "packet-definition-core"
    name = "PacketDefinition Core"
    description = "Data structures for low-level manual data manipulation"
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }
    }
}
