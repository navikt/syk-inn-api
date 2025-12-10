package no.nav.tsm.syk_inn_api.sykmelding

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.result
import arrow.core.right
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.SykmelderService
import no.nav.tsm.syk_inn_api.sykmelding.kafka.producer.SykmeldingKafkaMapper
import no.nav.tsm.syk_inn_api.sykmelding.kafka.producer.SykmeldingProducer
import no.nav.tsm.syk_inn_api.sykmelding.metrics.SykmeldingMetrics
import no.nav.tsm.syk_inn_api.sykmelding.metrics.SykmeldingServiceLevelIndicators
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleService
import no.nav.tsm.syk_inn_api.utils.failSpan
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.teamLogger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SykmeldingService(
    private val ruleService: RuleService,
    private val personService: PersonService,
    private val sykmelderService: SykmelderService,
    private val sykmeldingInputProducer: SykmeldingProducer,
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
    private val sykmeldingMetrics: SykmeldingMetrics,
    private val sli: SykmeldingServiceLevelIndicators,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    sealed class SykmeldingCreationErrors {
        object PersonDoesNotExist : SykmeldingCreationErrors()

        object PersistenceError : SykmeldingCreationErrors()

        object ResourceError : SykmeldingCreationErrors()
    }

    @Transactional
    @WithSpan
    fun createSykmelding(
        payload: OpprettSykmeldingPayload
    ): Either<SykmeldingCreationErrors, SykmeldingDocument> {
        val startTime = Instant.now()
        val span = Span.current()
        val sykmeldingId = UUID.randomUUID().toString()
        val mottatt = OffsetDateTime.now(ZoneOffset.UTC)

        val resources = result {
            val person = personService.getPersonByIdent(payload.meta.pasientIdent).bind()
            val sykmelder =
                sykmelderService
                    .sykmelderMedSuspensjon(
                        hpr = payload.meta.sykmelderHpr,
                        signaturDato = mottatt.toLocalDate(),
                        callId = sykmeldingId,
                    )
                    .bind()

            person to sykmelder
        }

        val (person, sykmelder) =
            resources.fold(
                { it },
                {
                    it.failSpan()
                    logger.error("Feil ved henting av eksterne ressurser: $it")
                    sykmeldingMetrics.incrementSykmeldingCreationFailed(
                        "ResourceError",
                        payload.meta.source
                    )
                    sli.recordFailedRequest("create")
                    return SykmeldingCreationErrors.ResourceError.left()
                },
            )

        val ruleValidationStart = Instant.now()
        val ruleResult: RegulaResult =
            ruleService.validateRules(
                payload = payload,
                sykmeldingId = sykmeldingId,
                sykmelder = sykmelder,
                foedselsdato = person.fodselsdato,
            )
        sykmeldingMetrics.recordRuleValidationDuration(
            Duration.between(ruleValidationStart, Instant.now())
        )

        val validation = SykmeldingKafkaMapper.mapValidationResult(ruleResult)
        val sykmeldingDocument =
            sykmeldingPersistenceService.saveSykmeldingPayload(
                sykmeldingId = sykmeldingId,
                mottatt = mottatt,
                payload = payload,
                person = person,
                sykmelder = sykmelder,
                ruleResult = validation,
            )

        sykmeldingInputProducer.send(
            sykmeldingId = sykmeldingId,
            sykmelding = sykmeldingDocument,
            person = person,
            sykmelder = sykmelder,
            validationResult = validation,
            source = payload.meta.source,
        )

        span.setAttribute("SykmeldingService.create.sykmeldingId", sykmeldingId)
        span.setAttribute("SykmeldingService.create.source", payload.meta.source)

        // Record metrics
        val duration = Duration.between(startTime, Instant.now())
        sykmeldingMetrics.recordCreateDuration(duration, payload.meta.source)
        sli.checkLatencySLO("create", duration)
        sli.recordSuccessfulRequest("create")

        val aktivitetType = payload.values.aktivitet.first()::class.simpleName ?: "UNKNOWN"

        // Extract fom/tom from aktivitet based on sealed interface implementation
        val aktivitetDates = payload.values.aktivitet.map { aktivitet ->
            when (aktivitet) {
                is OpprettSykmeldingAktivitet.IkkeMulig -> aktivitet.fom to aktivitet.tom
                is OpprettSykmeldingAktivitet.Gradert -> aktivitet.fom to aktivitet.tom
                is OpprettSykmeldingAktivitet.Behandlingsdager -> aktivitet.fom to aktivitet.tom
                is OpprettSykmeldingAktivitet.Avventende -> aktivitet.fom to aktivitet.tom
                is OpprettSykmeldingAktivitet.Reisetilskudd -> aktivitet.fom to aktivitet.tom
            }
        }

        val minFom = aktivitetDates.minOf { it.first }
        val maxTom = aktivitetDates.maxOf { it.second }
        val sykmeldingDays = ChronoUnit.DAYS.between(minFom, maxTom)
        sykmeldingMetrics.recordSykmeldingDuration(sykmeldingDays)

        sykmeldingMetrics.incrementSykmeldingCreated(
            source = payload.meta.source,
            diagnoseSystem = payload.values.hoveddiagnose.system,
            validationResult = validation.status.name,
            aktivitetType = aktivitetType,
            yrkesskade = payload.values.yrkesskade?.yrkesskade ?: false,
            svangerskapsrelatert = payload.values.svangerskapsrelatert,
            tilbakedateringPresent = payload.values.tilbakedatering != null,
        )

        return sykmeldingDocument.right()
    }

    @WithSpan
    fun getSykmeldingById(sykmeldingId: UUID): SykmeldingDocument? =
        sykmeldingPersistenceService.getSykmeldingById(sykmeldingId.toString())

    @WithSpan
    fun getSykmeldingerByIdent(ident: String): Result<List<SykmeldingDocument>> {
        teamLogger.info("Henter sykmeldinger for ident=$ident")

        val sykmeldinger: List<SykmeldingDocument> =
            sykmeldingPersistenceService.getSykmeldingerByIdent(ident)

        if (sykmeldinger.isEmpty()) {
            return Result.success(emptyList())
        }

        return Result.success(sykmeldinger)
    }

    @WithSpan
    fun verifySykmelding(
        payload: OpprettSykmeldingPayload
    ): Either<SykmeldingCreationErrors, RegulaResult> {
        val startTime = Instant.now()
        val sykmeldingId = UUID.randomUUID().toString()
        val mottatt = OffsetDateTime.now(ZoneOffset.UTC)

        val person: Person =
            personService.getPersonByIdent(payload.meta.pasientIdent).fold({ it }) {
                sykmeldingMetrics.incrementSykmeldingVerificationFailed(
                    "PersonDoesNotExist",
                    payload.meta.source
                )
                sli.recordFailedRequest("verify")
                return SykmeldingCreationErrors.PersonDoesNotExist.left()
            }

        val sykmelder =
            sykmelderService
                .sykmelderMedSuspensjon(
                    hpr = payload.meta.sykmelderHpr,
                    signaturDato = mottatt.toLocalDate(),
                    callId = sykmeldingId,
                )
                .fold({ it }) {
                    logger.error(
                        "Feil ved henting av sykmelder med hpr=${payload.meta.sykmelderHpr}"
                    )
                    it.failSpan()
                    sykmeldingMetrics.incrementSykmeldingVerificationFailed(
                        "ResourceError",
                        payload.meta.source
                    )
                    sli.recordFailedRequest("verify")
                    return SykmeldingCreationErrors.ResourceError.left()
                }

        val ruleResult: RegulaResult =
            ruleService.validateRules(
                payload = payload,
                sykmeldingId = sykmeldingId,
                sykmelder = sykmelder,
                foedselsdato = person.fodselsdato,
            )

        // Record metrics
        val duration = Duration.between(startTime, Instant.now())
        sykmeldingMetrics.recordVerifyDuration(duration, payload.meta.source)
        sli.checkLatencySLO("verify", duration)
        sli.recordSuccessfulRequest("verify")

        sykmeldingMetrics.incrementSykmeldingVerified(
            payload.meta.source,
            ruleResult.status.name
        )

        return ruleResult.right()
    }
}
