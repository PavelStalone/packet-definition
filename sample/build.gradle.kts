plugins {
    id("packet-definition-multiplatform")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(project(":packet-definition"))
            }
        }
    }
}
