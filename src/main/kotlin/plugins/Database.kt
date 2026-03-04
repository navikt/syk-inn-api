package plugins

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.core.Environment
import no.nav.tsm.core.db.runFlywayMigrations
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabase() {
    val env: Environment by dependencies

    runFlywayMigrations(env.postgres.url, env.postgres.username, env.postgres.password)

    Database.connect(
        url = env.postgres.url,
        user = env.postgres.username,
        password = env.postgres.password,
    )
}
