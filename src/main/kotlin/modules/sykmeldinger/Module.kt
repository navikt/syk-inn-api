package no.nav.tsm.modules.sykmeldinger

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.modules.sykmeldinger.db.SykmeldingExposedRepo

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
        provide<SykmeldingExposedRepo>(SykmeldingExposedRepo::class)
        provide<SykmeldingService>(SykmeldingService::class)
    }
}
