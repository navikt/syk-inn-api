package plugins.auth

import io.ktor.server.application.*
import io.ktor.server.plugins.di.dependencies

fun Application.configureAuthentication() {
    dependencies { provide(TexasClient::class) }

    configureInternalSymfoniUserAuth()
}
