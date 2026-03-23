package no.nav.tsm.modules.sykmeldinger

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import java.time.LocalDate
import java.util.UUID
import no.nav.tsm.modules.sykmeldinger.db.SykmeldingerRepo
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.pdl.PdlClient
import no.nav.tsm.modules.sykmeldinger.rules.RuleService
import no.nav.tsm.modules.sykmeldinger.sykmelder.SykmelderService

class SykmeldingerService(
    private val pdlClient: PdlClient,
    private val sykmelderService: SykmelderService,
    private val ruleService: RuleService,
    private val repo: SykmeldingerRepo,
) {
    enum class VerifyErrors {
        PersonNotInPdl,
        UnknownResourceError,
    }

    suspend fun verify(
        sykmelding: UnverifiedSykInnSykmelding
    ): Either<VerifyErrors, SykInnSykmeldingRuleResult> = either {
        val sykmelder =
            catch({ sykmelderService.byHpr(sykmelding.meta.behandlerHpr, LocalDate.now()) }) {
                raise(VerifyErrors.UnknownResourceError)
            }

        val pasient =
            ensureNotNull(pdlClient.getPerson(sykmelding.meta.pasientIdent)) {
                VerifyErrors.PersonNotInPdl
            }

        return ruleService.verify(sykmelding, sykmelder, pasient).right()
    }

    suspend fun create(sykmelding: UnverifiedSykInnSykmelding): VerifiedSykInnSykmelding {
        val sykmelder = sykmelderService.byHpr(sykmelding.meta.behandlerHpr, LocalDate.now())
        val pasient =
            pdlClient.getPerson(sykmelding.meta.pasientIdent)
                ?: throw IllegalStateException("Unable to execute rules for pasient not in PDL")
        val rules = ruleService.verify(sykmelding, sykmelder, pasient)

        val verified = sykmelding.toVerifiedSykmelding(rules, sykmelder)

        repo.insertSykmelding(verified)

        return verified
    }

    fun byId(sykmeldingId: UUID): VerifiedSykInnSykmelding {
        TODO("implement")
    }

    fun byIdent(ident: String): List<VerifiedSykInnSykmelding> {
        // TODO: need to call PDL to get all idents for ident

        return repo.sykmeldinger(ident)
    }
}
