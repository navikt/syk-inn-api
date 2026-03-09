package modules.sykmeldinger

import io.ktor.server.application.Application
import modules.sykmeldinger.sykmelder.configureSykmelderModule

fun Application.configureSykmeldingerModule() {
    configureSykmelderModule()
    configureSykmeldingerDependencies()
}
