package no.nav.tsm.utils

import io.ktor.client.HttpClient
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.mockk.mockk
import java.util.Properties
import no.nav.tsm.core.Environment
import no.nav.tsm.core.PostgresConfig
import no.nav.tsm.core.Runtime
import no.nav.tsm.core.RuntimeEnvironments
import no.nav.tsm.modules.behandler.configureBehandlerModule
import no.nav.tsm.modules.sykmeldinger.configureSykmeldingerModule
import no.nav.tsm.plugins.configureDatabase
import no.nav.tsm.plugins.configureDependencies
import no.nav.tsm.plugins.configureSerialization
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer

fun Application.configureIntegrationTestDependencies(
    postgres: PostgreSQLContainer,
    kafka: KafkaContainer? = null,
) {
    // Integration test specific Environment configu
    dependencies {
        provide<Environment>() {
            Environment(
                runtime = Runtime(env = RuntimeEnvironments.LOCAL, name = "test-app"),
                postgres =
                    PostgresConfig(
                        url = postgres.jdbcUrl,
                        username = postgres.username,
                        password = postgres.password,
                    ),
                kafka = if (kafka != null) Properties() else mockk(),
                texas = { mockk() },
                external = { mockk() },
                auth = { mockk() },
            )
        }
    }

    // Global
    configureDependencies()
    configureDatabase()
    configureSerialization()

    // Modules
    configureSykmeldingerModule()
    configureBehandlerModule()
}

fun Application.configureMockedEnvironment() {
    dependencies {
        provide<HttpClient> { HttpClient() }
        provide<Environment>() {
            Environment(
                runtime = Runtime(env = RuntimeEnvironments.LOCAL, name = "test-app"),
                kafka = mockk(),
                postgres = mockk(),
                texas = { mockk() },
                external = { mockk() },
                auth = { mockk() },
            )
        }
    }
}
