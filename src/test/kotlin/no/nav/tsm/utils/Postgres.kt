package no.nav.tsm.utils

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.postgresql.PostgreSQLContainer

class Postgres {
    val container = PostgreSQLContainer("postgres:17-alpine").apply { start() }
    val config = createIntegrationEnvironment(container)

    fun runMigrations(clean: Boolean = false) {
        val flyway =
            Flyway.configure()
                .dataSource(
                    config.postgres.jdbc,
                    config.postgres.username,
                    config.postgres.password,
                )
                .defaultSchema(config.postgres.schema)
                .cleanDisabled(false)
                .createSchemas(true)
                .locations("db/migrations")
                .load()

        if (clean) {
            flyway.clean()
        }
        flyway.migrate()
    }

    fun connect() {
        Database.connect(
            url = config.postgres.jdbc,
            user = config.postgres.username,
            password = config.postgres.password,
        )
    }
}
