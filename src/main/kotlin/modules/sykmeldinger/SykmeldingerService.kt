package modules.sykmeldinger

import java.time.LocalDate
import java.util.UUID
import modules.sykmeldinger.db.SykmeldingerRepo
import modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import modules.sykmeldinger.rules.RuleService
import modules.sykmeldinger.sykmelder.SykmelderService

class SykmeldingerService(
    private val sykmelderService: SykmelderService,
    private val ruleService: RuleService,
    private val repo: SykmeldingerRepo,
) {

    suspend fun verify(sykmelding: UnverifiedSykInnSykmelding): SykInnSykmeldingRuleResult {
        val sykmelder = sykmelderService.byHpr(sykmelding.meta.behandlerHpr, LocalDate.now())
        return ruleService.verify(sykmelding, sykmelder)
    }

    suspend fun create(sykmelding: UnverifiedSykInnSykmelding): VerifiedSykInnSykmelding {
        val sykmelder = sykmelderService.byHpr(sykmelding.meta.behandlerHpr, LocalDate.now())
        val rules = ruleService.verify(sykmelding, sykmelder)

        val verified = sykmelding.toVerifiedSykmelding(rules, sykmelder)

        repo.insertSykmelding(verified)

        return verified
    }

    suspend fun insert(sykmelding: VerifiedSykInnSykmelding): VerifiedSykInnSykmelding {
        TODO("implement")
    }

    fun byId(sykmeldingId: UUID): VerifiedSykInnSykmelding {
        TODO("implement")
    }

    fun byIdent(ident: String): List<VerifiedSykInnSykmelding> {
        // TODO: need to call PDL to get all idents for ident

        return repo.sykmeldinger(ident)
    }
}
