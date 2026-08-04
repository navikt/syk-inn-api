package no.nav.tsm.modules.sykmeldinger.sykmelder

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.ktor.di.dynamicDependencies
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
