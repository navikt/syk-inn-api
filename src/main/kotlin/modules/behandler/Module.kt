package modules.behandler

import io.ktor.server.application.Application

fun Application.configureBehandlerModule() {
    configureBehandlerDependencies()
    configureBehandlerRoutes()
}
