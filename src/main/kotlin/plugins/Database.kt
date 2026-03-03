package no.nav.tsm.plugins

import io.ktor.server.application.Application
import no.nav.tsm.core.db.runFlywayMigrations
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabase() {
    val url = environment.config.property("postgres.url").getString()
    val user = environment.config.property("postgres.username").getString()
    val password = environment.config.property("postgres.password").getString()

    runFlywayMigrations(url, user, password)

    Database.connect(url = url, user = user, password = password)
}
