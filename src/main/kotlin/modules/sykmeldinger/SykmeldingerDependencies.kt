package modules.sykmeldinger

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import modules.sykmeldinger.db.SykmeldingerRepo

fun Application.configureSykmeldingerDependencies() {
    dependencies {
        provide<SykmeldingerRepo>(SykmeldingerRepo::class)
        provide<SykmeldingerService>(SykmeldingerService::class)
    }
}
