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

rootProject.name = "KDocHub"

include("kdochub-core:base")
include("kdochub-core:database")
include("kdochub-core:access")
include("kdochub-core")
include("kdochub-document")
include("kdochub-server")
