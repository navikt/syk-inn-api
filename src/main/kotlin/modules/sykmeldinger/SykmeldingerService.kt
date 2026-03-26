package no.nav.tsm.modules.sykmeldinger

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import arrow.fx.coroutines.parZip
import java.time.LocalDate
import java.util.UUID
import no.nav.tsm.core.logger
import no.nav.tsm.modules.sykmeldinger.db.SykmeldingRepo
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.pdl.PdlClient
import no.nav.tsm.modules.sykmeldinger.rules.RuleService
import no.nav.tsm.modules.sykmeldinger.sykmelder.Sykmelder
import no.nav.tsm.modules.sykmeldinger.sykmelder.SykmelderService

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
            ruleService
                .verify(sykmelding, sykmelder, pasient)
                .mapLeft { CreateErrors.RuleError }
                .bind()
        }
    }

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

        /**
         * parZip runs both these resource lookups in parallel, executes the rules and gives us our
         * actual verified domain sykmelding.
         */
        val verified: VerifiedSykInnSykmelding =
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
                val rules =
                    ruleService
                        .verify(sykmelding, sykmelder, pasient)
                        .mapLeft { CreateErrors.RuleError }
                        .bind()

                ensure(sykmelder is Sykmelder.MedSuspensjon) {
                    logger.error(
                        "Behandler that does not exist in HPR/Btsys tried to create a sykmelding! :O"
                    )

                    CreateErrors.UnknownResourceError
                }

                sykmelding.toVerifiedSykmelding(rules, sykmelder)
            }

        val created = repo.insert(sykmelding.submitId, verified)

        created.fold({
            /**
             * If we hit the idempotency constraint, we need to get the existing sykmelding by
             * idempotency and return it.
             */
            val existing = repo.byIdempotencyKey(sykmelding.submitId)
            logger.info("Idempotency Key ${sykmelding.submitId} hit constraint")
            ensureNotNull(existing) {
                logger.error(
                    "Idempotency Key ${sykmelding.submitId} hit constraint but doesn't exist, seems sus"
                )
                CreateErrors.UnknownResourceError
            }
            existing
        }) {
            logger.info("Sykmelding with ID ${verified.sykmeldingId} was created successfully!")
            verified
        }
    }

    enum class GetErrors {
        NotFound,
        UnknownError,
    }

    fun byId(sykmeldingId: UUID): Either<GetErrors, VerifiedSykInnSykmelding> {
        val sykmelding = repo.byId(sykmeldingId) ?: return GetErrors.NotFound.left()

        return sykmelding.right()
    }

    fun byIdent(ident: String): Either<GetErrors, List<VerifiedSykInnSykmelding>> {
        // TODO: need to call PDL to get all idents for ident
        return repo.allByIdent(ident).right()
    }
}
