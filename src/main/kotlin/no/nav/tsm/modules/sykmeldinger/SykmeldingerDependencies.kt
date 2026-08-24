package no.nav.tsm.modules.sykmeldinger

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingRepo
import no.nav.tsm.modules.sykmeldinger.pdl.PdlArrowed
import no.nav.tsm.modules.sykmeldinger.rules.RuleService
import no.nav.tsm.pdl.plugin.PdlPlugin

fun Application.configureSykmeldingerDependencies() {
    install(PdlPlugin)

    dependencies {
        provide(PdlArrowed::class)
        provide(RuleService::class)
        provide(SykmeldingRepo::class)
        provide(SykmeldingerService::class)
    }
}
