package no.nav.tsm.core.db

import no.nav.tsm.core.PostgresConfig
import org.flywaydb.core.Flyway

fun runFlywayMigrations(postgresConfig: PostgresConfig) {
    val flyway =
        Flyway.configure()
            .cleanDisabled(false)
            .dataSource(
                "jdbc:${postgresConfig.url}",
                postgresConfig.username,
                postgresConfig.password,
            )
            .defaultSchema(postgresConfig.schema)
            .createSchemas(true)
            .locations("db/migrations")
            .load()

    // In this development period, we'll edit the first changeset and always nuke DB
    flyway.clean()

    // Always migrate
    flyway.migrate()
}
