package no.nav.tsm.core.db

import io.ktor.server.config.ApplicationConfig
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties


fun connectToPostgres(config: ApplicationConfig): Connection {
    val url = config.property("postgres.url").getString()
    val user = config.property("postgres.username").getString()
    val password = config.property("postgres.password").getString()

    runFlywayMigrations(url, user, password)

    val properties = Properties().apply {
        setProperty("user", user)
        setProperty("password", password)
    }

    return DriverManager.getConnection(url, properties)
}
