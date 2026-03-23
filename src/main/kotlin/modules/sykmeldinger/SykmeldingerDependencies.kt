package no.nav.tsm.modules.sykmeldinger

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.core.dynamicDependencies
import no.nav.tsm.modules.sykmeldinger.db.SykmeldingerRepo
import no.nav.tsm.modules.sykmeldinger.pdl.PdlCloudClient
import no.nav.tsm.modules.sykmeldinger.pdl.PdlLocalClient
import no.nav.tsm.modules.sykmeldinger.rules.RuleService

fun Application.configureSykmeldingerDependencies() {
    dynamicDependencies {
        local { provide(PdlLocalClient::class) }
        cloud { provide(PdlCloudClient::class) }
    }

    dependencies {
        provide<RuleService>(RuleService::class)
        provide<SykmeldingerRepo>(SykmeldingerRepo::class)
        provide<SykmeldingerService>(SykmeldingerService::class)
    }
}
