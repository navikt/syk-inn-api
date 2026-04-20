package no.nav.tsm.utils

import no.nav.tsm.core.db.runFlywayMigrations
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer

abstract class WithPostgresql {
    companion object {
        val postgres = PostgreSQLContainer("postgres:17-alpine").apply { start() }

        val config = createIntegrationEnvironment(postgres, null)

        fun runMigrations(clean: Boolean = false) {
            runFlywayMigrations(config.postgres)
        }

        fun connect() {
            R2dbcDatabase.connect(
                url = config.postgres.r2.url,
                user = config.postgres.username,
                password = config.postgres.password,
            )
        }
    }
}

abstract class WithAll : WithPostgresql() {
    companion object {
        val kafka = ConfluentKafkaContainer("confluentinc/cp-kafka:8.1.0").apply { start() }
    }
}
