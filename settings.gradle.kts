pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "packet-definition"

include(":sample")
include(":benchmarks")
include(":packet-definition")
include(":packet-definition-core")

project(":packet-definition-core").projectDir = file("./core")
