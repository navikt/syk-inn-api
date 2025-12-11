package no.nav.tsm.syk_inn_api.sykmelding

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.result
import arrow.core.right
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelder.SykmelderService
import no.nav.tsm.syk_inn_api.sykmelding.kafka.producer.SykmeldingKafkaMapper
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMapper
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingDb
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingRepository
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeta
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentRuleResult
import no.nav.tsm.syk_inn_api.sykmelding.response.toSykmeldingDocumentSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.response.toSykmeldingDocumentValues
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleService
import no.nav.tsm.syk_inn_api.utils.failSpan
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.teamLogger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.SykmeldingType
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class SykmeldingService(
    private val ruleService: RuleService,
    private val personService: PersonService,
    private val sykmelderService: SykmelderService,
    private val sykmeldingRepository: SykmeldingRepository,
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    sealed class SykmeldingCreationErrors {
        data object PersonDoesNotExist : SykmeldingCreationErrors()

        data object PersistenceError : SykmeldingCreationErrors()

        data object ResourceError : SykmeldingCreationErrors()

        data object AlreadyExists : SykmeldingCreationErrors()
    }

    @WithSpan
    fun createSykmelding(
        payload: OpprettSykmeldingPayload
    ): Either<SykmeldingCreationErrors, SykmeldingDocument> {
        val span = Span.current()
        val sykmeldingId = UUID.randomUUID()
        val mottatt = OffsetDateTime.now(ZoneOffset.UTC)

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

        val validation = SykmeldingKafkaMapper.mapValidationResult(ruleResult)

        try {

            val sykmeldingDb =
                mapSykmeldingPayloadToDatabaseEntity(
                    sykmeldingId = sykmeldingId.toString(),
                    mottatt = mottatt,
                    payload = payload,
                    pasient = person,
                    sykmelder = sykmelder,
                    ruleResult = validation,
                )

            val savedEntity = sykmeldingPersistenceService.saveSykmelding(sykmeldingDb)
            val sykmeldingDocument = mapDatabaseEntityToSykmeldingDocument(savedEntity)

            span.setAttribute("SykmeldingService.create.sykmeldingId", sykmeldingId.toString())
            span.setAttribute("SykmeldingService.create.source", payload.meta.source)

            return sykmeldingDocument.right()
        } catch (e: DataIntegrityViolationException) {
            logger.warn("Sykmelding with idempotencyKey=${payload.submitId} already exists", e)
            return SykmeldingCreationErrors.AlreadyExists.left()
        }
    }

    @WithSpan
    fun getSykmeldingById(sykmeldingId: UUID): SykmeldingDocument? =
        sykmeldingRepository.findSykmeldingEntityBySykmeldingId(sykmeldingId.toString())?.let {
            mapDatabaseEntityToSykmeldingDocument(it)
        }

    @WithSpan
    fun getSykmeldingerByIdent(ident: String): Result<List<SykmeldingDocument>> {
        teamLogger.info("Henter sykmeldinger for ident=$ident")

        val sykmeldinger: List<SykmeldingDocument> =
            sykmeldingRepository.findAllByPasientIdent(ident).map {
                mapDatabaseEntityToSykmeldingDocument(it)
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

    fun updateSykmelding(
        sykmeldingId: String,
        sykmeldingRecord: SykmeldingRecord,
        person: Person,
        sykmelder: Sykmelder,
    ) {
        val sykmeldingEntity = sykmeldingRepository.findSykmeldingEntityBySykmeldingId(sykmeldingId)

        val typeNotDigital = sykmeldingRecord.sykmelding.type != SykmeldingType.DIGITAL
        if (sykmeldingEntity == null && typeNotDigital) {
            try {
                val entity =
                    mapSykmeldingRecordToSykmeldingDatabaseEntity(
                        sykmeldingId = sykmeldingId,
                        sykmeldingRecord = sykmeldingRecord,
                        validertOk = true,
                        person = person,
                        sykmelder = sykmelder,
                    )
                sykmeldingRepository.save(entity)
            } catch (ex: Exception) {
                logger.error(
                    "Failed to map SykmeldingRecord to SykmeldingDb for sykmeldingId=$sykmeldingId",
                    ex,
                )
                throw IllegalStateException(
                    "Failed to map SykmeldingRecord to SykmeldingDb for sykmeldingId=$sykmeldingId",
                    ex,
                )
            }
        }

        if (sykmeldingRecord.sykmelding.type == SykmeldingType.DIGITAL) {
            val updatedEntity = sykmeldingEntity?.copy(validertOk = true)
            logger.info("Updating sykmelding with id=${sykmeldingRecord.sykmelding.id}")
            sykmeldingRepository.save(
                updatedEntity
                    ?: mapSykmeldingRecordToSykmeldingDatabaseEntity(
                        sykmeldingId = sykmeldingId,
                        sykmeldingRecord = sykmeldingRecord,
                        validertOk = true,
                        person = person,
                        sykmelder = sykmelder,
                    ),
            )
            logger.info("Updated sykmelding with id=${sykmeldingRecord.sykmelding.id}")
        }
    }

    fun deleteSykmelding(sykmeldingId: String) {
        sykmeldingRepository.deleteBySykmeldingId(sykmeldingId)
        logger.info("Deleted sykmelding with id=$sykmeldingId")
    }

    fun deleteSykmeldingerOlderThanDays(daysToSubtract: Long): Int {
        val cutoffDate = LocalDate.now().minusDays(daysToSubtract)
        return sykmeldingRepository.deleteSykmeldingerWithAktivitetOlderThan(cutoffDate)
    }

    companion object {
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
            ruleResult: ValidationResult,
        ): SykmeldingDb {
            val persistedSykmelding =
                PersistedSykmeldingMapper.mapSykmeldingPayloadToPersistedSykmelding(
                    payload,
                    sykmeldingId,
                    pasient,
                    sykmelder,
                    ruleResult,
                )
            return SykmeldingDb(
                sykmeldingId = sykmeldingId,
                pasientIdent = payload.meta.pasientIdent,
                sykmelderHpr = payload.meta.sykmelderHpr,
                mottatt = mottatt,
                sykmelding = persistedSykmelding,
                legekontorOrgnr = payload.meta.legekontorOrgnr,
                legekontorTlf = payload.meta.legekontorTlf,
                fom = persistedSykmelding.aktivitet.minOf { it.fom },
                tom = persistedSykmelding.aktivitet.maxOf { it.tom },
                idempotencyKey = payload.submitId,
            )
        }

        fun mapSykmeldingRecordToSykmeldingDatabaseEntity(
            sykmeldingId: String,
            sykmeldingRecord: SykmeldingRecord,
            validertOk: Boolean,
            person: Person,
            sykmelder: Sykmelder,
        ): SykmeldingDb {
            val persistedSykmelding =
                PersistedSykmeldingMapper.mapSykmeldingRecordToPersistedSykmelding(
                    sykmeldingRecord,
                    person,
                    sykmelder,
                )
            return SykmeldingDb(
                sykmeldingId = sykmeldingId,
                mottatt = sykmeldingRecord.sykmelding.metadata.mottattDato,
                pasientIdent = sykmeldingRecord.sykmelding.pasient.fnr,
                sykmelderHpr = sykmelder.hpr,
                sykmelding = persistedSykmelding,
                legekontorOrgnr = PersistedSykmeldingMapper.mapLegekontorOrgnr(sykmeldingRecord),
                legekontorTlf = PersistedSykmeldingMapper.mapLegekontorTlf(sykmeldingRecord),
                validertOk = validertOk,
                fom = persistedSykmelding.aktivitet.minOf { it.fom },
                tom = persistedSykmelding.aktivitet.maxOf { it.tom },
                idempotencyKey = UUID.randomUUID(),
            )
        }
    }
}
