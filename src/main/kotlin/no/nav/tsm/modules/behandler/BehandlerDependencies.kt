package no.nav.tsm.modules.behandler

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.modules.behandler.access.BehandlerAccessControlService

fun Application.configureBehandlerDependencies() {
    dependencies {
        provide(
            _root_ide_package_.no.nav.tsm.modules.behandler.access
                .BehandlerAccessControlService::class
        )
    }
}
