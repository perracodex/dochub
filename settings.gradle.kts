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

include("kdochub-system:base")
include("kdochub-system:database")
include("kdochub-system:access")
include("kdochub-document")
include("kdochub-server")
