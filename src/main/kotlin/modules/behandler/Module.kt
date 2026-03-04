package modules.behandler

import io.ktor.server.application.Application
import modules.behandler.api.configureBehandlerRoutes
import plugins.configureSerialization

fun Application.configureBehandlerModule() {
    configureSerialization()
    configureBehandlerRoutes()
}
