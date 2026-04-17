package no.nav.tsm.utils

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.mockk.mockk
import java.util.Properties
import kotlin.time.Duration.Companion.hours
import no.nav.tsm.core.Environment
import no.nav.tsm.core.KafkaConfig
import no.nav.tsm.core.KafkaInputProducer
import no.nav.tsm.core.KafkaSykmeldingConsumer
import no.nav.tsm.core.PostgresConfig
import no.nav.tsm.core.Runtime
import no.nav.tsm.core.RuntimeEnvironments
import no.nav.tsm.core.SykmeldingConfig
import no.nav.tsm.module
import no.nav.tsm.plugins.auth.configureAuthentication
import no.nav.tsm.plugins.configureDatabase
import no.nav.tsm.plugins.configureDependencies
import no.nav.tsm.plugins.configureSerialization
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer

fun Application.configurePostgresIntegrationTests(postgres: PostgreSQLContainer) {
    // Integration test specific Environment configuration
    dependencies { provide<Environment>() { createIntegrationEnvironment(postgres, null) } }

    // Global
    configureAuthentication()
    configureDependencies()
    configureDatabase()
    configureSerialization()

    // #1: Postgres specific tests will have to provide their own "in test" set of modules
}

fun Application.configureFullIntegrationTests(
    postgres: PostgreSQLContainer,
    kafka: ConfluentKafkaContainer,
) {
    // Integration test specific Environment configuration
    dependencies { provide<Environment>() { createIntegrationEnvironment(postgres, kafka) } }

    // #2: Postgresql + Kafka tests just set up the entire application
    module()
}

private fun createIntegrationEnvironment(
    postgres: PostgreSQLContainer,
    kafka: ConfluentKafkaContainer?,
) =
    Environment(
        runtime = Runtime(env = RuntimeEnvironments.LOCAL, name = "test-app"),
        postgres =
            PostgresConfig(
                url = postgres.jdbcUrl.removePrefix("jdbc:"),
                username = postgres.username,
                password = postgres.password,
                schema = "public",
            ),
        sykmeldingConfig = SykmeldingConfig(retention = 1.hours),
        kafka =
            KafkaConfig(
                config =
                    if (kafka != null)
                        Properties().apply { this["bootstrap.servers"] = kafka.bootstrapServers }
                    else mockk(),
                inputProducer = KafkaInputProducer(delay = 500),
                sykmeldingConsumer = KafkaSykmeldingConsumer(longPoll = 1000L),
            ),
        texas = { mockk() },
        external = { mockk() },
        auth = { mockk() },
    )
