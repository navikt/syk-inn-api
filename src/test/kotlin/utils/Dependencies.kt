package utils

import core.Environment
import core.PostgresConfig
import core.Runtime
import core.RuntimeEnvironments
import io.ktor.client.HttpClient
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.mockk.mockk
import java.util.Properties
import modules.behandler.configureBehandlerModule
import modules.sykmeldinger.configureSykmeldingerModule
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import plugins.configureDatabase
import plugins.configureDependencies
import plugins.configureSerialization

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

fun Application.configureTestEnvironment() {
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
