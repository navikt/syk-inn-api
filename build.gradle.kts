import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val javaVersion = JvmTarget.JVM_21

// Runtime deps
val logstashLogbackEncoderVersion = "9.0"
val openHtmlToPdfVersion = "1.1.31"
val verapdfVersion = "1.28.2"
val kotlinxHtmlVersion = "0.12.0"
val arrowVersion = "2.2.0"
val regulaVersion = "47"
val sykmeldingInputVersion = "22"
val diagnoserVersion = "2026.1.10"
val hypersistenceUtilsHibernateVersion = "3.14.1"

// Dev deps
val testContainersVersion = "2.0.2"
val ktfmtVersion = "0.44"
val mockkVersion = "1.14.6"
val mockkSpringVersion = "4.0.2"
val mockwebserverVersion = "5.3.0"
val otelVersion = "1.56.0"
val otelAnnotationsVersion = "2.21.0"

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    id("org.springframework.boot") version "4.0.1"

    // Other plugins
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.0.0"
    id("com.github.ben-manes.versions") version "0.53.0"
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
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:${kotlinxHtmlVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-html:${kotlinxHtmlVersion}")
    implementation("org.postgresql:postgresql")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.prometheus:prometheus-metrics-simpleclient-bridge:1.0.0")

    implementation("no.nav.tsm.regulus:regula:$regulaVersion")
    implementation("no.nav.tsm.sykmelding:input:$sykmeldingInputVersion")
    implementation("no.nav.tsm:diagnoser:$diagnoserVersion")
    implementation("io.arrow-kt:arrow-core:$arrowVersion")

    implementation("io.opentelemetry:opentelemetry-api:$otelVersion")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:$otelAnnotationsVersion")
    implementation("io.hypersistence:hypersistence-utils-hibernate-71:$hypersistenceUtilsHibernateVersion")
    implementation("io.github.openhtmltopdf:openhtmltopdf-pdfbox:$openHtmlToPdfVersion")
    implementation("io.github.openhtmltopdf:openhtmltopdf-slf4j:$openHtmlToPdfVersion")
    implementation("io.github.openhtmltopdf:openhtmltopdf-svg-support:$openHtmlToPdfVersion")
    implementation("org.verapdf:validation-model-jakarta:$verapdfVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:testcontainers-postgresql:$testContainersVersion")
    testImplementation("org.testcontainers:testcontainers-kafka:$testContainersVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("com.squareup.okhttp3:mockwebserver3-junit5:$mockwebserverVersion")
    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("com.ninja-squad:springmockk:${mockkSpringVersion}")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

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
val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}
