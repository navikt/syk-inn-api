package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.core.Environment
import no.nav.tsm.core.db.getFlyway
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabase() {
    val env: Environment by dependencies

    getFlyway(env.postgres).migrate()

    Database.connect(
        url = env.postgres.jdbc,
        user = env.postgres.username,
        password = env.postgres.password,
        databaseConfig = DatabaseConfig.invoke { defaultSchema = Schema(env.postgres.schema) },
    )
}
