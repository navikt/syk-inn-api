package no.nav.tsm.modules.sykmeldinger.sykmelder

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.ktor.di.dynamicDependencies
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.behandler.TsmBehandlerCloudClient
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.behandler.TsmBehandlerLocalClient

fun Application.configureSykmelderDependencies() {
    dynamicDependencies {
        local { provide(TsmBehandlerLocalClient::class) }
        cloud { provide(TsmBehandlerCloudClient::class) }
    }

    dependencies { provide(SykmelderService::class) }
}
