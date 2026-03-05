package core.db

import org.flywaydb.core.Flyway

fun runFlywayMigrations(url: String, user: String, password: String) {
    val flyway =
        Flyway.configure()
            .cleanDisabled(false)
            .dataSource(url, user, password)
            .locations("db/migrations")
            .load()

    // In this development period, we'll edit the first changeset and always nuke DB
    flyway.clean()

    // Always migrate
    flyway.migrate()
}
