package modules.sykmeldinger

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import modules.sykmeldinger.db.SykmeldingerRepo
import modules.sykmeldinger.rules.RuleService

fun Application.configureSykmeldingerDependencies() {
    dependencies {
        provide<RuleService>(RuleService::class)
        provide<SykmeldingerRepo>(SykmeldingerRepo::class)
        provide<SykmeldingerService>(SykmeldingerService::class)
    }
}
