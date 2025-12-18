package no.nav.tsm.syk_inn_api.sykmelding

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.result
import arrow.core.right
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelder.SykmelderService
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMapper
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykInnPersistence
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingDb
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeta
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentRuleResult
import no.nav.tsm.syk_inn_api.sykmelding.response.toSykmeldingDocumentSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.response.toSykmeldingDocumentValues
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleService
import no.nav.tsm.syk_inn_api.utils.failSpan
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.teamLogger
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class SykmeldingService(
    private val ruleService: RuleService,
    private val personService: PersonService,
    private val sykmelderService: SykmelderService,
    private val sykInnPersistence: SykInnPersistence,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    sealed class SykmeldingCreationErrors {
        data object PersonDoesNotExist : SykmeldingCreationErrors()

        data object ProcessingError : SykmeldingCreationErrors()

        data object PersistenceError : SykmeldingCreationErrors()

        data object ResourceError : SykmeldingCreationErrors()

        data class AlreadyExists(val sykmeldingDocument: SykmeldingDocument) :
            SykmeldingCreationErrors()
    }

    @WithSpan
    fun createSykmelding(
        payload: OpprettSykmeldingPayload
    ): Either<SykmeldingCreationErrors, SykmeldingDocument> {
        val span = Span.current()
        val sykmeldingId = UUID.randomUUID()
        val mottatt = OffsetDateTime.now(ZoneOffset.UTC)
        val existing = sykInnPersistence.getSykmeldingByIdempotencyKey(payload.submitId)
        if (existing != null) {
            return duplicateSubmitResult(payload, existing)
        }

        val resources = result {
            val person = personService.getPersonByIdent(payload.meta.pasientIdent).bind()
            val sykmelder =
                sykmelderService
                    .sykmelderMedSuspensjon(
                        hpr = payload.meta.sykmelderHpr,
                        signaturDato = mottatt.toLocalDate(),
                        callId = sykmeldingId.toString(),
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
                    return SykmeldingCreationErrors.ResourceError.left()
                },
            )

        val ruleResult: RegulaResult =
            ruleService.validateRules(
                payload = payload,
                sykmeldingId = sykmeldingId.toString(),
                sykmelder = sykmelder,
                foedselsdato = person.fodselsdato,
            )

        try {

            val sykmeldingDb =
                mapSykmeldingPayloadToDatabaseEntity(
                    sykmeldingId = sykmeldingId.toString(),
                    mottatt = mottatt,
                    payload = payload,
                    pasient = person,
                    sykmelder = sykmelder,
                    ruleResult = ruleResult,
                )

            val saved = sykInnPersistence.saveNewSykmelding(sykmeldingDb, null)

            span.setAttribute("SykmeldingService.create.sykmeldingId", sykmeldingId.toString())
            span.setAttribute("SykmeldingService.create.source", payload.meta.source)

            return mapDatabaseEntityToSykmeldingDocument(saved).right()
        } catch (ex: DataIntegrityViolationException) {
            logger.warn(
                "Sykmelding med submitId=${payload.submitId} allerede er lagret i databasen",
                ex
            )
            return duplicateSubmitResult(
                payload = payload,
                existing = sykInnPersistence.getSykmeldingByIdempotencyKey(payload.submitId)
            )
        }
    }

    private fun duplicateSubmitResult(
        payload: OpprettSykmeldingPayload,
        existing: SykmeldingDb?
    ): Either<SykmeldingCreationErrors, Nothing> {
        logger.warn(
            "Sykmelding med submitId=${payload.submitId} allerede er lagret i databasen",
        )

        existing ?: return SykmeldingCreationErrors.ProcessingError.left()

        val pasientId = existing.pasientIdent
        val hprNr = existing.sykmelderHpr

        if (hprNr != payload.meta.sykmelderHpr) {
            return SykmeldingCreationErrors.ProcessingError.left()
        } else if (pasientId != payload.meta.pasientIdent) {
            return SykmeldingCreationErrors.ProcessingError.left()
        }

        return SykmeldingCreationErrors.AlreadyExists(
                mapDatabaseEntityToSykmeldingDocument(existing)
            )
            .left()
    }

    @WithSpan
    fun getSykmeldingById(sykmeldingId: UUID): SykmeldingDocument? =
        sykInnPersistence.getBySykmeldingId(sykmeldingId.toString())?.let {
            mapDatabaseEntityToSykmeldingDocument(it)
        }

    @WithSpan
    fun getSykmeldingerByIdent(ident: String): Result<List<SykmeldingDocument>> {
        teamLogger.info("Henter sykmeldinger for ident=$ident")

        val sykmeldinger: List<SykmeldingDocument> =
            sykInnPersistence.getSykmeldingByPasientIdent(ident).map {
                mapDatabaseEntityToSykmeldingDocument(it)
            }

        if (sykmeldinger.isEmpty()) {
            return Result.success(emptyList())
        }

        return Result.success(sykmeldinger)
    }

    @WithSpan
    fun verifySykmelding(
        payload: OpprettSykmeldingPayload
    ): Either<SykmeldingCreationErrors, RegulaResult> {
        val sykmeldingId = UUID.randomUUID().toString()
        val mottatt = OffsetDateTime.now(ZoneOffset.UTC)

        val person: Person =
            personService.getPersonByIdent(payload.meta.pasientIdent).fold({ it }) {
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
                    return SykmeldingCreationErrors.ResourceError.left()
                }

        val ruleResult: RegulaResult =
            ruleService.validateRules(
                payload = payload,
                sykmeldingId = sykmeldingId,
                sykmelder = sykmelder,
                foedselsdato = person.fodselsdato,
            )

        return ruleResult.right()
    }
}

fun mapDatabaseEntityToSykmeldingDocument(sykmeldingDb: SykmeldingDb): SykmeldingDocument {
    val persistedSykmelding = sykmeldingDb.sykmelding
    return SykmeldingDocument(
        sykmeldingId = sykmeldingDb.sykmeldingId,
        meta =
            SykmeldingDocumentMeta(
                mottatt = sykmeldingDb.mottatt,
                pasientIdent = persistedSykmelding.pasient.ident,
                sykmelder = persistedSykmelding.sykmelder.toSykmeldingDocumentSykmelder(),
                legekontorOrgnr = sykmeldingDb.legekontorOrgnr,
                legekontorTlf = sykmeldingDb.legekontorTlf,
            ),
        values = persistedSykmelding.toSykmeldingDocumentValues(),
        utfall =
            persistedSykmelding.regelResultat.let {
                SykmeldingDocumentRuleResult(
                    result = it.result,
                    melding = it.meldingTilSender,
                )
            },
    )
}

fun mapSykmeldingPayloadToDatabaseEntity(
    sykmeldingId: String,
    mottatt: OffsetDateTime,
    payload: OpprettSykmeldingPayload,
    pasient: Person,
    sykmelder: Sykmelder,
    ruleResult: RegulaResult,
): SykmeldingDb {
    val validationResult = PersistedSykmeldingMapper.mapValidationResult(ruleResult)

    val persistedSykmelding =
        PersistedSykmeldingMapper.mapSykmeldingPayloadToPersistedSykmelding(
            payload,
            sykmeldingId,
            pasient,
            sykmelder,
            validationResult,
        )

    return SykmeldingDb(
        sykmeldingId = sykmeldingId,
        idempotencyKey = payload.submitId,
        pasientIdent = payload.meta.pasientIdent,
        sykmelderHpr = payload.meta.sykmelderHpr,
        mottatt = mottatt,
        sykmelding = persistedSykmelding,
        legekontorOrgnr = payload.meta.legekontorOrgnr,
        legekontorTlf = payload.meta.legekontorTlf,
        fom = persistedSykmelding.aktivitet.minOf { it.fom },
        tom = persistedSykmelding.aktivitet.maxOf { it.tom },
        validationResult = validationResult,
    )
}
