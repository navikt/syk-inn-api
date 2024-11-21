import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.tasks.bundling.BootJar

val javaVersion = JvmTarget.JVM_21

val logstashLogbackEncoderVersion = "8.0"
val tokenSupportVersion = "5.0.11"
val ktfmtVersion = "0.44"
val openapiVersion = "2.6.0"
val syfoXmlCodegenVersion = "2.0.1"
val jaxbApiVersion = "2.3.1"
val jaxbVersion = "2.4.0-b180830.0438"
val javaxActivationVersion = "1.1.1"
val diagnosekoderVersion = "1.2024.1"

group = "no.nav.tsm"
version = "1.0.0"

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.diffplug.spotless") version "6.25.0"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$openapiVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")
    implementation("org.hibernate.validator:hibernate-validator")
    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("no.nav.security:token-client-spring:$tokenSupportVersion")

    implementation("no.nav.helse.xml:xmlfellesformat:$syfoXmlCodegenVersion")
    implementation("no.nav.helse.xml:kith-hodemelding:$syfoXmlCodegenVersion")
    implementation("no.nav.helse.xml:sm2013:$syfoXmlCodegenVersion")

    implementation("javax.xml.bind:jaxb-api:$jaxbApiVersion")
    implementation("org.glassfish.jaxb:jaxb-runtime:$jaxbVersion")
    implementation("javax.activation:activation:$javaxActivationVersion")

    implementation("no.nav.helse:diagnosekoder:$diagnosekoderVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.squareup.okhttp3:mockwebserver")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        jvmTarget.set(javaVersion)
    }
}


tasks {

    withType<BootJar> {
        archiveFileName = "app.jar"
    }

    withType<Test> {
        useJUnitPlatform {
        }
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    task("addPreCommitGitHookOnBuild") {
        println("⚈ ⚈ ⚈ Running Add Pre Commit Git Hook Script on Build ⚈ ⚈ ⚈")
        exec {
            commandLine("cp", "./.scripts/pre-commit", "./.git/hooks")
        }
        println("✅ Added Pre Commit Git Hook Script.")
    }

    configure<SpotlessExtension> {
        kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }

}
