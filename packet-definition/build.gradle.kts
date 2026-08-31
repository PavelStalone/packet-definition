plugins {
    id("packet-definition-multiplatform")
    id("packet-definition-publish")
}

artifact {
    id = "packet-definition"
    name = "PacketDefinition DSL"
    description = "Type-safe DSL for defining and working with binary data packets"
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                api(project(":packet-definition-core"))
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
