package modules.sykmeldinger

import core.dynamicDependencies
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import modules.sykmeldinger.db.SykmeldingerRepo
import modules.sykmeldinger.pdl.PdlCloudClient
import modules.sykmeldinger.pdl.PdlLocalClient
import modules.sykmeldinger.rules.RuleService

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
