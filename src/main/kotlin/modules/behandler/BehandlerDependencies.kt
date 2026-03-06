package modules.behandler

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import modules.behandler.access.BehandlerAccessControlService

fun Application.configureBehandlerDependencies() {
    dependencies { provide(BehandlerAccessControlService::class) }
}
