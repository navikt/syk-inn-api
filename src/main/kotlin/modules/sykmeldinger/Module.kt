package no.nav.tsm.modules.sykmeldinger

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies

fun Application.configureSykmeldingerApi() {
    // TODO:
    // AUTH
    // ROUTES
    // PDF (NEI)

    configureSykmeldingDependencies()
    configureSykmeldingRoutes()
}

private fun Application.configureSykmeldingDependencies() {
    dependencies {
        provide<SykmeldingRepo>(SykmeldingRepo::class)
        provide<SykmeldingService>(SykmeldingService::class)
    }
}
