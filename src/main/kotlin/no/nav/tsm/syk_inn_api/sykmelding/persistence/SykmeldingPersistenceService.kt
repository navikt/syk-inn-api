package no.nav.tsm.syk_inn_api.sykmelding.persistence

import java.time.LocalDate
import java.time.OffsetDateTime
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingPayload
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeta
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentRuleResult
import no.nav.tsm.syk_inn_api.sykmelding.response.toSykmeldingDocumentSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.response.toSykmeldingDocumentValues
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.SykmeldingType
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import org.springframework.stereotype.Service

@Service
class SykmeldingPersistenceService(
    private val sykmeldingRepository: SykmeldingRepository,
) {
    private val logger = logger()

    fun getSykmeldingById(sykmeldingId: String): SykmeldingDocument? {
        return sykmeldingRepository.findSykmeldingEntityBySykmeldingId(sykmeldingId)?.let {
            mapDatabaseEntityToSykmeldingDocument(it)
        }
    }

    fun saveSykmeldingPayload(
        sykmeldingId: String,
        mottatt: OffsetDateTime,
        payload: OpprettSykmeldingPayload,
        person: Person,
        sykmelder: Sykmelder,
        ruleResult: ValidationResult,
    ): SykmeldingDocument? {
        logger.info("Lagrer sykmelding med id=${sykmeldingId}")
        val savedEntity =
            sykmeldingRepository.save(
                mapSykmeldingPayloadToDatabaseEntity(
                    sykmeldingId = sykmeldingId,
                    mottatt = mottatt,
                    payload = payload,
                    pasient = person,
                    sykmelder = sykmelder,
                    ruleResult = ruleResult,
                ),
            )

        logger.info("Sykmelding with id=$sykmeldingId er lagret")

        return mapDatabaseEntityToSykmeldingDocument(savedEntity)
    }

    private fun mapSykmeldingPayloadToDatabaseEntity(
        sykmeldingId: String,
        mottatt: OffsetDateTime,
        payload: OpprettSykmeldingPayload,
        pasient: Person,
        sykmelder: Sykmelder,
        ruleResult: ValidationResult,
    ): SykmeldingDb {
        logger.info("Mapper sykmelding payload til database entitet for sykmeldingId=$sykmeldingId")
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
        )
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
        )
    }

    fun getSykmeldingerByIdent(ident: String): List<SykmeldingDocument> {
        return sykmeldingRepository.findAllByPasientIdent(ident).map {
            mapDatabaseEntityToSykmeldingDocument(it)
        }
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
            logger.info("Sykmelding with id=$sykmeldingId is not found in DB, creating new entry")
            try {
                // TODO skal sjekke om den faktisk er avvist eller ikkje før en kan sette validert
                // ok?
                val entity =
                    mapSykmeldingRecordToSykmeldingDatabaseEntity(
                        sykmeldingId = sykmeldingId,
                        sykmeldingRecord = sykmeldingRecord,
                        validertOk = true,
                        person = person,
                        sykmelder = sykmelder,
                    )
                sykmeldingRepository.save(entity)
                logger.debug("Saved new sykmelding with id=${sykmeldingRecord.sykmelding.id}")
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
        val cutoffDate = java.time.LocalDate.now().minusDays(daysToSubtract)
        return sykmeldingRepository.deleteSykmeldingerWithAktivitetOlderThan(cutoffDate)
    }
}
