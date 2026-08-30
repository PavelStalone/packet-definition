plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.benchmark)
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.benchmark.runtime)
                implementation(project(":packet-definition"))
            }
        }
    }
}

benchmark {
    configurations {
        named("main") {
            warmups = 50
            iterations = 100
            iterationTime = 1
            iterationTimeUnit = "ms"
            outputTimeUnit = "ms"
            reportFormat = "json"
        }
    }

    targets {
        register("jvm")
    }
}
