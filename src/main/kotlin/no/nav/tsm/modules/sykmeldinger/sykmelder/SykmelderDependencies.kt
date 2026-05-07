package no.nav.tsm.modules.sykmeldinger.sykmelder

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.core.dynamicDependencies
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.btsys.BtsysCloudClient
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.btsys.BtsysLocalClient
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr.HprCloudClient
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr.HprLocalClient

fun Application.configureSykmelderDependencies() {
    dynamicDependencies {
        local {
            provide(BtsysLocalClient::class)
            provide(HprLocalClient::class)
        }
        cloud {
            provide(BtsysCloudClient::class)
            provide(HprCloudClient::class)
        }
    }

    dependencies { provide(SykmelderService::class) }
}
