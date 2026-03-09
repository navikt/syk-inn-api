package plugins.auth

import io.ktor.server.application.*
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.plugins.auth.configureMachineTokenAuth

fun Application.configureAuthentication() {
    dependencies { provide(TexasClient::class) }

    configureMachineTokenAuth()
    configureInternalSymfoniUserAuth()
}
