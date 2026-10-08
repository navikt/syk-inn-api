import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import dev.detekt.gradle.Detekt

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.flyway)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

group = "no.nav.tsm"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Ktor
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.di)
    implementation(ktorLibs.server.callId)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.jackson3)
    implementation(ktorLibs.server.metrics.micrometer)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.apache5)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.client.callId)
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)
    implementation(libs.caffeine)

    // TSM libraries
    implementation(libs.tsm.sykmeldinger.input)
    implementation(libs.tsm.diagnoser)
    implementation(libs.tsm.regula)
    implementation(libs.tsm.pdl.client)
    implementation(tsmKtorLibs.core)
    implementation(tsmKtorLibs.auth)
    implementation(tsmKtorLibs.kafka)
    implementation(tsmKtorLibs.kafka.sykmeldinger)

    // Database and such
    implementation(libs.flyway.postgres)
    implementation(libs.flyway.core)
    implementation(libs.postgresql)
    implementation(exposedLibs.core)
    implementation(exposedLibs.jdbc)
    implementation(exposedLibs.json)
    implementation(exposedLibs.java.time)
    implementation(libs.kafka.client)

    // Monitoring and logging
    implementation(libs.logback.classic)
    implementation(libs.logback.encoder)

    // Test
    testImplementation(tsmKtorLibs.kafka.test)
    testImplementation(ktorLibs.server.testHost)
    testImplementation(ktorLibs.client.mock)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.kotest.assertions)
}

val generateBuildInfo =
    tasks.register("generateBuildInfo") {
        val gitSha = providers.environmentVariable("GITHUB_SHA").orNull ?: "local"
        inputs.property("gitSha", gitSha)
        val outputDir = layout.buildDirectory.dir("generated/kotlin")
        outputs.dir(outputDir)

        if (gitSha == "local") {
            logger.warn("GITHUB_SHA not found, using 'local' as git SHA, this will fail if deployed!")
        }

        doLast {
            val file = outputDir.get().file("BuildInfo.kt").asFile
            file.parentFile.mkdirs()
            file.writeText(
                """
            package no.nav.tsm.core

            internal object BuildInfo {
                const val GIT_SHA = "$gitSha"
            }
            """
                    .trimIndent()
            )
        }
    }

sourceSets["main"].kotlin.srcDir(generateBuildInfo.map { it.outputs.files })

tasks {
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles {}
        from("src/main/resources/logback.xml") {
            into("/")
        }
    }

    configure<SpotlessExtension> {
        kotlin { ktfmt("0.62").kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }

    named<DependencyUpdatesTask>("dependencyUpdates") {
        fun String.isNonStable(): Boolean {
            val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { uppercase().contains(it) }
            val regex = "^[0-9,.v-]+(-r)?$".toRegex()
            val isStable = stableKeyword || regex.matches(this)
            return isStable.not()
        }

        rejectVersionIf {
            candidate.version.isNonStable()
        }
    }
}

tasks.register<Exec>("preRunLocal") {
    group = "application"
    commandLine("./scripts/pre-dev.sh")
}

tasks.register<JavaExec>("runLocal") {
    group = "application"
    mainClass.set("io.ktor.server.netty.EngineMain")
    classpath = sourceSets["main"].runtimeClasspath

    args("-config=application-local.conf")
    jvmArgs("-Dio.ktor.development=true", "-Dlogback.configurationFile=logback-local.xml")

    dependsOn("preRunLocal")
}

tasks.withType<Detekt>().configureEach {
    config.setFrom(file("detekt.yml"))
    buildUponDefaultConfig = true

    dependsOn("spotlessApply")
}

/**
 * Disable auto running of detekt on build and stuff
 */
afterEvaluate {
    tasks.named("check") {
        setDependsOn(dependsOn.filter { !it.toString().contains("detekt") })
    }
}
