package no.nav.tsm.modules.sykmeldinger

import io.ktor.server.application.Application
import no.nav.tsm.modules.sykmeldinger.sykmelder.configureSykmelderModule

fun Application.configureSykmeldingerModule() {
    configureSykmelderModule()
    configureSykmeldingerDependencies()
}
