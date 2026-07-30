package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.ktor.auth.entra.EntraAuth
import no.nav.tsm.ktor.auth.texas.Texas

fun Application.configureAuthentication() {
    dependencies { provide(Texas::class) }

    install(EntraAuth) {
        machine = true
        obo = true
        autoStub = true
    }
}
