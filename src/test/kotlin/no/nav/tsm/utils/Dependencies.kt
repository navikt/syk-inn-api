package no.nav.tsm.utils

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import no.nav.tsm.core.DeleterJob
import no.nav.tsm.core.Environment
import no.nav.tsm.core.JobsConfig
import no.nav.tsm.core.KafkaSykmeldingConsumer
import no.nav.tsm.core.PostgresConfig
import no.nav.tsm.core.ProducerJob
import no.nav.tsm.core.Runtime
import no.nav.tsm.core.SykmeldingConfig
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.module
import no.nav.tsm.plugins.configureAuthentication
import no.nav.tsm.plugins.configureDependencies
import no.nav.tsm.plugins.configureSerialization
import org.testcontainers.postgresql.PostgreSQLContainer

fun Application.configurePostgresIntegrationTests(postgres: PostgreSQLContainer) {
    // Integration test specific Environment configuration
    dependencies { provide<Environment>() { createIntegrationEnvironment(postgres) } }

    // Global
    configureAuthentication()
    configureDependencies()
    configureSerialization()

    // #1: Postgres specific tests will have to provide their own "in test" set of modules
}

fun ApplicationTestBuilder.configureFullIntegrationTests(postgres: PostgreSQLContainer) {
    // Integration test specific Environment configuration
    application.dependencies { provide<Environment>() { createIntegrationEnvironment(postgres) } }

    // #2: Postgresql + Kafka tests just set up the entire application
    application.module()
}

fun createIntegrationEnvironment(postgres: PostgreSQLContainer) =
    Environment(
        runtime = Runtime(env = RuntimeCluster.LOCAL, name = "test-app", version = "testy-v0"),
        postgres =
            PostgresConfig(
                jdbc = postgres.jdbcUrl,
                username = postgres.username,
                password = postgres.password,
                schema = "public",
            ),
        sykmeldingConsumer = KafkaSykmeldingConsumer(longPoll = 1000.milliseconds),
        sykmeldingConfig = SykmeldingConfig(retention = 14.days),
        jobs =
            JobsConfig(
                inputProducer = ProducerJob(delay = 500.milliseconds, hungTimeout = 1.hours),
                juridiskProducer = ProducerJob(delay = 500.milliseconds, hungTimeout = 1.hours),
                sykmeldingDeleter = DeleterJob(interval = 250.milliseconds),
            ),
        external = { mockk() },
    )
