package no.nav.tsm.core.db

import org.flywaydb.core.Flyway

fun runFlywayMigrations(
    url: String,
    user: String,
    password: String
) {
    val flyway = Flyway.configure()
        .dataSource(url, user, password)
        .locations("db/migrations")
        .load()

    flyway.migrate()
}
