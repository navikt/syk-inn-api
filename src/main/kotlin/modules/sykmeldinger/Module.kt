package no.nav.tsm.modules.sykmeldinger

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.DependencyKey
import io.ktor.server.plugins.di.create
import io.ktor.server.plugins.di.dependencies

fun Application.configureSykmeldingerApi() {
    dependencies {
        provide<SykmeldingRepo>(SykmeldingRepo::class)
        provide<SykmeldingService>(SykmeldingService::class)
    }

    // DEPS
    // AUTH
    // ROUTES
    // PDF (NEI)

    // val repo by SykmeldingRepo

    configureSykmeldingRoutes()
}
