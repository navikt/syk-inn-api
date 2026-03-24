package no.nav.tsm.modules.sykmeldinger

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import arrow.fx.coroutines.parZip
import java.time.LocalDate
import java.util.UUID
import no.nav.tsm.modules.sykmeldinger.db.SykmeldingRepo
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
    private val repo: SykmeldingRepo,
) {
    enum class VerifyErrors {
        PersonNotInPdl,
        UnknownResourceError,
    }

    suspend fun verify(
        sykmelding: UnverifiedSykInnSykmelding
    ): Either<VerifyErrors, SykInnSykmeldingRuleResult> = either {
        parZip(
            {
                sykmelderService
                    .byHpr(sykmelding.meta.behandlerHpr, LocalDate.now())
                    .mapLeft { VerifyErrors.UnknownResourceError }
                    .bind()
            },
            {
                pdlClient
                    .getPerson(sykmelding.meta.pasientIdent)
                    .mapLeft {
                        when (it) {
                            PdlClient.PdlErrors.NotFound -> VerifyErrors.PersonNotInPdl
                            PdlClient.PdlErrors.UnknownError -> VerifyErrors.UnknownResourceError
                        }
                    }
                    .bind()
            },
        ) { sykmelder, pasient ->
            ruleService.verify(sykmelding, sykmelder, pasient)
        }
    }

    suspend fun create(
        sykmelding: UnverifiedSykInnSykmelding
    ): Either<VerifyErrors, VerifiedSykInnSykmelding> = either {
        val (sykmelder, rules) =
            parZip(
                {
                    sykmelderService
                        .byHpr(sykmelding.meta.behandlerHpr, LocalDate.now())
                        .mapLeft { VerifyErrors.UnknownResourceError }
                        .bind()
                },
                {
                    pdlClient
                        .getPerson(sykmelding.meta.pasientIdent)
                        .mapLeft {
                            when (it) {
                                PdlClient.PdlErrors.NotFound -> VerifyErrors.PersonNotInPdl
                                PdlClient.PdlErrors.UnknownError ->
                                    VerifyErrors.UnknownResourceError
                            }
                        }
                        .bind()
                },
            ) { sykmelder, pasient ->
                sykmelder to ruleService.verify(sykmelding, sykmelder, pasient)
            }

        val verified = sykmelding.toVerifiedSykmelding(rules, sykmelder)

        repo.insertSykmelding(verified)

        return verified.right()
    }

    fun byId(sykmeldingId: UUID): VerifiedSykInnSykmelding {
        TODO("implement")
    }

    fun byIdent(ident: String): List<VerifiedSykInnSykmelding> {
        // TODO: need to call PDL to get all idents for ident

        return repo.sykmeldinger(ident)
    }
}
