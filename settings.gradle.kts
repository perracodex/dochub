pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // https://github.com/gradle/foojay-toolchains
    // https://github.com/gradle/foojay-toolchains/tags
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "KDoc"

include("kdoc-system:core")
include("kdoc-system:database")
include("kdoc-system:access")
include("kdoc-document")
include("kdoc-server")
