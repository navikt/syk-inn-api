import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.tasks.bundling.BootJar

val javaVersion = JvmTarget.JVM_21

// Runtime deps
val logstashLogbackEncoderVersion = "8.1"
val openHtmlToPdfVersion = "1.1.29"
val verapdfVersion = "1.28.2"
val kotlinxHtmlVersion = "0.12.0"
val arrowVersion = "2.1.2"
val regulaVersion = "41"
val sykmeldingInputVersion = "13"

// Dev deps
val testContainersVersion = "1.21.3"
val ktfmtVersion = "0.44"
val mockkVersion = "1.14.5"

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    id("org.springframework.boot") version "3.5.4"

    // Other plugins
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.2.1"
    id("com.github.ben-manes.versions") version "0.52.0"
}

group = "no.nav.tsm"
version = "1.0.0"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories {
    mavenCentral()
    maven { url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.postgresql:postgresql")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("no.nav.tsm.regulus:regula:$regulaVersion")
    implementation("no.nav.tsm.sykmelding", "input", sykmeldingInputVersion)
    implementation("io.arrow-kt:arrow-core:$arrowVersion")

    // PDF generation dependencies:
    implementation("io.github.openhtmltopdf:openhtmltopdf-pdfbox:$openHtmlToPdfVersion")
    implementation("io.github.openhtmltopdf:openhtmltopdf-slf4j:$openHtmlToPdfVersion")
    implementation("io.github.openhtmltopdf:openhtmltopdf-svg-support:$openHtmlToPdfVersion")
    implementation("org.verapdf:validation-model-jakarta:$verapdfVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:${kotlinxHtmlVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-html:${kotlinxHtmlVersion}")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.kafka:spring-kafka-test")
//    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:postgresql:$testContainersVersion")
    testImplementation("org.testcontainers:kafka:$testContainersVersion")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("io.mockk:mockk:${mockkVersion}")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget.set(javaVersion)
    }
}

tasks {

    build { dependsOn("bootJar") }

    withType<BootJar> {
        archiveFileName = "app.jar"
    }

    withType<Test> {
        useJUnitPlatform {}
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        systemProperty("spring.profiles.active", "test")
    }

    register<Exec>("addPreCommitGitHookOnBuild") {
        doFirst {
            println("⚈ ⚈ ⚈ Running Add Pre Commit Git Hook Script on Build ⚈ ⚈ ⚈")
        }
        commandLine("cp", "./.scripts/pre-commit", "./.git/hooks")
        doLast {
            println("✅ Added Pre Commit Git Hook Script.")
        }
    }

    configure<SpotlessExtension> {
        kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }
}
