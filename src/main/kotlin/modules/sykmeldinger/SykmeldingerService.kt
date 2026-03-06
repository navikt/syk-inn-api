package modules.sykmeldinger

import java.util.UUID
import modules.sykmeldinger.db.SykmeldingerRepo
import modules.sykmeldinger.domain.RuledSykInnSykmelding
import modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import modules.sykmeldinger.domain.UnruledSykInnSykmelding
import modules.sykmeldinger.rules.RuleService

class SykmeldingerService(val ruleService: RuleService, val repo: SykmeldingerRepo) {
    fun test() = repo.test()

    fun createBoio() = repo.createBoio()

    suspend fun verify(sykmelding: UnruledSykInnSykmelding): SykInnSykmeldingRuleResult {
        return ruleService.verify(sykmelding)
    }

    suspend fun create(sykmelding: UnruledSykInnSykmelding): RuledSykInnSykmelding {
        val rules = ruleService.verify(sykmelding)

        repo.insertSykmelding(sykmelding, rules)

        TODO("implement")
    }

    suspend fun insert(sykmelding: RuledSykInnSykmelding): RuledSykInnSykmelding {
        TODO("implement")
    }

    fun byId(sykmeldingId: UUID): RuledSykInnSykmelding {
        TODO("implement")
    }

    fun byIdent(ident: String): List<RuledSykInnSykmelding> {
        TODO("implement")
    }
}
