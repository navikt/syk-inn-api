package no.nav.tsm.plugins.auth

import io.ktor.server.application.*
import no.nav.tsm.ktor.auth.entra.EntraAuth

fun Application.configureMachineTokenAuth() {
    install(EntraAuth) { autoStub = true }
}
