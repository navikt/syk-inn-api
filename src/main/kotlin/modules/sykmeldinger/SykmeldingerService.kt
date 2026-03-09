package modules.sykmeldinger

import java.util.UUID
import modules.sykmeldinger.db.SykmeldingerRepo
import modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import modules.sykmeldinger.rules.RuleService

class SykmeldingerService(val ruleService: RuleService, val repo: SykmeldingerRepo) {
    fun test() = repo.test()

    fun createBoio() = repo.createBoio()

    suspend fun verify(sykmelding: UnverifiedSykInnSykmelding): SykInnSykmeldingRuleResult {
        return ruleService.verify(sykmelding)
    }

    suspend fun create(sykmelding: UnverifiedSykInnSykmelding): VerifiedSykInnSykmelding {
        val rules = ruleService.verify(sykmelding)

        repo.insertSykmelding(sykmelding, rules)

        TODO("implement")
    }

    suspend fun insert(sykmelding: VerifiedSykInnSykmelding): VerifiedSykInnSykmelding {
        TODO("implement")
    }

    fun byId(sykmeldingId: UUID): VerifiedSykInnSykmelding {
        TODO("implement")
    }

    fun byIdent(ident: String): List<VerifiedSykInnSykmelding> {
        TODO("implement")
    }
}
