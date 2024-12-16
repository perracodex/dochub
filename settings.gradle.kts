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

rootProject.name = "DocHub"

include("dochub-core:base")
include("dochub-core:database")
include("dochub-core:access")
include("dochub-core")
include("dochub-document")
include("dochub-server")
