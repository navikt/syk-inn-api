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
    enum class CreateErrors {
        PersonNotInPdl,
        UnknownResourceError,
    }

    suspend fun verify(
        sykmelding: UnverifiedSykInnSykmelding
    ): Either<CreateErrors, SykInnSykmeldingRuleResult> = either {
        parZip(
            {
                sykmelderService
                    .byHpr(sykmelding.meta.behandlerHpr, LocalDate.now())
                    .mapLeft { CreateErrors.UnknownResourceError }
                    .bind()
            },
            {
                pdlClient
                    .getPerson(sykmelding.meta.pasientIdent)
                    .mapLeft {
                        when (it) {
                            PdlClient.PdlErrors.NotFound -> CreateErrors.PersonNotInPdl
                            PdlClient.PdlErrors.UnknownError -> CreateErrors.UnknownResourceError
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
    ): Either<CreateErrors, VerifiedSykInnSykmelding> = either {
        parZip(
            {
                sykmelderService
                    .byHpr(sykmelding.meta.behandlerHpr, LocalDate.now())
                    .mapLeft { CreateErrors.UnknownResourceError }
                    .bind()
            },
            {
                pdlClient
                    .getPerson(sykmelding.meta.pasientIdent)
                    .mapLeft {
                        when (it) {
                            PdlClient.PdlErrors.NotFound -> CreateErrors.PersonNotInPdl
                            PdlClient.PdlErrors.UnknownError ->
                                CreateErrors.UnknownResourceError
                        }
                    }
                    .bind()
            },
        ) { sykmelder, pasient ->
            val rules = ruleService.verify(sykmelding, sykmelder, pasient)
            val verified = sykmelding.toVerifiedSykmelding(rules, sykmelder)

            repo.insertSykmelding(verified)

            verified
        }
    }

    enum class GetErrors {
        NotFound,
        UnknownError,
    }

    fun byId(sykmeldingId: UUID): Either<GetErrors, VerifiedSykInnSykmelding> {
        TODO("implement")
    }

    fun byIdent(ident: String): Either<GetErrors, List<VerifiedSykInnSykmelding>> {
        // TODO: need to call PDL to get all idents for ident
        return repo.sykmeldinger(ident).right()
    }
}
