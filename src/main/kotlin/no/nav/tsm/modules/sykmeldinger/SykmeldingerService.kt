package no.nav.tsm.modules.sykmeldinger

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.context.bind
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import arrow.fx.coroutines.parZip
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import java.util.*
import no.nav.tsm.core.logger
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingRepo
import no.nav.tsm.modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.pdl.PdlClient
import no.nav.tsm.modules.sykmeldinger.pdl.PdlPerson
import no.nav.tsm.modules.sykmeldinger.rules.RuleService
import no.nav.tsm.modules.sykmeldinger.rules.toSykInnRuleResult
import no.nav.tsm.modules.sykmeldinger.sykmelder.Sykmelder
import no.nav.tsm.modules.sykmeldinger.sykmelder.SykmelderService
import no.nav.tsm.regulus.regula.RegulaResult

class SykmeldingerService(
    private val pdlClient: PdlClient,
    private val sykmelderService: SykmelderService,
    private val ruleService: RuleService,
    private val repo: SykmeldingRepo,
) {
    private val logger = logger()

    enum class CreateErrors {
        PersonNotInPdl,
        RuleError,
        UnknownResourceError,
    }

    suspend fun verify(sykmelding: UnverifiedSykInnSykmelding): Either<CreateErrors, RegulaResult> =
        getSykmeldingVerifyResources(sykmelding) { sykmelder, previous, pasient ->
            ruleService
                .verify(sykmelding, previous, sykmelder, pasient)
                .mapLeft { CreateErrors.RuleError }
                .map { (result, _) -> result }
                .bind()
        }

    @WithSpan
    suspend fun create(
        sykmelding: UnverifiedSykInnSykmelding
    ): Either<CreateErrors, VerifiedSykInnSykmelding> = either {
        val alreadyExists = repo.byIdempotencyKey(sykmelding.submitId)
        if (alreadyExists != null) {
            logger.info(
                "Idempotency Key ${sykmelding.submitId} lookup found something, returning early"
            )
            return@either alreadyExists
        }

        getSykmeldingVerifyResources<VerifiedSykInnSykmelding>(sykmelding) {
                sykmelder,
                previous,
                pasient ->
                ensure(sykmelder is Sykmelder.MedSuspensjon) {
                    logger.error(
                        "Behandler that does not exist in HPR/Btsys tried to create a sykmelding! :O"
                    )

                    CreateErrors.UnknownResourceError
                }
                val (rules, juridiskVurdering) =
                    ruleService
                        .verify(sykmelding, previous, sykmelder, pasient)
                        .mapLeft { CreateErrors.RuleError }
                        .bind()

                val verifiedSykmelding =
                    sykmelding.toVerifiedSykmelding(rules.toSykInnRuleResult(), sykmelder, pasient)
                val inserted =
                    repo.insert(
                        submitKey = sykmelding.submitId,
                        sykmelding = verifiedSykmelding,
                        juridisk = juridiskVurdering,
                        ruleResult = rules,
                    )

                inserted.fold(
                    {
                        /**
                         * If we hit the idempotency constraint, we need to get the existing
                         * sykmelding by idempotency and return it.
                         */
                        val existing = repo.byIdempotencyKey(sykmelding.submitId)

                        logger.info(
                            "Idempotency Key ${sykmelding.submitId} hit constraint (cause: ${it})"
                        )
                        ensureNotNull(existing) {
                            logger.error(
                                "Idempotency Key ${sykmelding.submitId} hit constraint but doesn't exist, seems sus"
                            )
                            CreateErrors.UnknownResourceError
                        }
                        existing
                    },
                    {
                        logger.info(
                            "Sykmelding with ID ${it.sykmeldingId} was created successfully!"
                        )
                        it
                    },
                )
            }
            .bind()
    }

    enum class GetErrors {
        NotFound,
        UnknownError,
    }

    suspend fun byId(sykmeldingId: UUID): Either<GetErrors, VerifiedSykInnSykmelding> {
        val sykmelding = repo.byId(sykmeldingId) ?: return GetErrors.NotFound.left()

        return sykmelding.right()
    }

    @WithSpan
    suspend fun byIdent(ident: String): Either<GetErrors, List<VerifiedSykInnSykmelding>> {
        return repo.allByIdent(ident).right()
    }

    /** Fetches behandler (hpr) and sykmeldt (pdl) in parallel. */
    private suspend fun <Result> getSykmeldingVerifyResources(
        sykmelding: UnverifiedSykInnSykmelding,
        block:
            suspend Raise<CreateErrors>.(
                sykmelder: Sykmelder, previous: List<VerifiedSykInnSykmelding>, pasient: PdlPerson,
            ) -> Result,
    ): Either<CreateErrors, Result> = either {
        /**
         * parZip runs both these resource lookups in parallel, executes the rules and gives us our
         * actual verified domain sykmelding.
         */
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
            {
                byIdent(sykmelding.meta.pasientIdent)
                    .mapLeft { CreateErrors.UnknownResourceError }
                    .bind()
            },
        ) { sykmelder, pasient, previous ->
            block(sykmelder, previous, pasient)
        }
    }
}
