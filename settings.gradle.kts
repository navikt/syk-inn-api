rootProject.name = "syk-inn-api"

val ktorVersion = "3.5.2"
val tsmKtorVersion = "1.0.17"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }

    versionCatalogs {
        create("ktorLibs").from("io.ktor:ktor-version-catalog:${ktorVersion}")
        create("tsmKtorLibs").from("no.nav.tsm:ktor-version-catalog:${tsmKtorVersion}")
    }
}

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

plugins {
    id("io.github.ben-manes.versions.settings") version "0.58.0"
}