rootProject.name = "syk-inn-api"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }

    versionCatalogs {
        create("ktorLibs").from("io.ktor:ktor-version-catalog:3.5.1")
    }
}
