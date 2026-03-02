package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.core.db.connectToPostgres
import java.sql.Connection

fun Application.configureDependencies() {
    val config = environment.config

    dependencies {
        provide<Connection> { connectToPostgres(config) }
    }
}
