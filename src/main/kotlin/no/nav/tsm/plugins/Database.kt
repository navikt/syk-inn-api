package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.core.Environment
import no.nav.tsm.core.db.runFlywayMigrations
import no.nav.tsm.core.logger
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

private val logger = logger()

fun Application.configureDatabase() {
    val env: Environment by dependencies

    logger.info("Database URL: ${env.postgres.url}")

    runFlywayMigrations(env.postgres)

    R2dbcDatabase.connect(
        url =
            "r2dbc:${env.postgres.url}"
                .let { url ->
                    val separator = if ('?' in url) "&" else "?"
                    "$url${separator}schema=${env.postgres.schema}"
                },
        user = env.postgres.username,
        password = env.postgres.password,
    )
}
