package plugins

import core.Environment
import core.db.runFlywayMigrations
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
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
