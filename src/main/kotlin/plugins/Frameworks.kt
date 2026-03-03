package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import java.sql.Connection

fun Application.configureDependencies() {
    val config = environment.config

    dependencies {
        // TODO, should handle our own Database instansce?
        // provide<Connection> { connectToPostgres(config) }
    }
}
