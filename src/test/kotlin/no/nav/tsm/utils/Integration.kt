package no.nav.tsm.utils

import no.nav.tsm.core.PostgresConfig
import no.nav.tsm.core.db.runFlywayMigrations
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer

abstract class WithPostgresql {
    companion object {
        val postgres = PostgreSQLContainer("postgres:17-alpine").apply { start() }

        val postgresConfig =
            PostgresConfig(
                url = postgres.jdbcUrl.removePrefix("jdbc:"),
                r2 = postgres.jdbcUrl.removePrefix("jdbc:"),
                username = postgres.username,
                password = postgres.password,
                schema = "public",
                sslCert = "",
                sslKeyPk8 = "",
            )

        fun runMigrations(clean: Boolean = false) {
            runFlywayMigrations(postgresConfig)
        }

        fun connect() {
            R2dbcDatabase.connect(
                url = "r2dbc:${postgresConfig.url}",
                user = postgresConfig.username,
                password = postgresConfig.password,
            )
        }
    }
}

abstract class WithAll : WithPostgresql() {
    companion object {
        val kafka = ConfluentKafkaContainer("confluentinc/cp-kafka:8.1.0").apply { start() }
    }
}
