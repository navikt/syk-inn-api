package no.nav.tsm.plugins.auth

import io.ktor.server.application.*
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.ktor.auth.texas.TexasClient

fun Application.configureAuthentication() {
    dependencies { provide(TexasClient::class) }

    configureMachineTokenAuth()
    configureInternalSymfoniUserAuth()
}
