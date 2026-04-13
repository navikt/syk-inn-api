package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.core.Environment
import no.nav.tsm.core.db.runFlywayMigrations
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

fun Application.configureDatabase() {
    val env: Environment by dependencies

    runFlywayMigrations(env.postgres.url, env.postgres.username, env.postgres.password)

    R2dbcDatabase.connect(
        url = env.postgres.r2dbUrl,
        user = env.postgres.username,
        password = env.postgres.password,
    )
}
