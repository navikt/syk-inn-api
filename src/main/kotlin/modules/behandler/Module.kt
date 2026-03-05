package modules.behandler

import io.ktor.server.application.Application
import modules.behandler.api.configureBehandlerRoutes

fun Application.configureBehandlerModule() {
    configureBehandlerRoutes()
}
