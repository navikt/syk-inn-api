package no.nav.tsm.syk_inn_api.sykmelding.persistence

import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.exception.SykmeldingDBMappingException
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HelsenettProxyService
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingPayload
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecord
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingType
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMapper
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeta
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SykmeldingPersistenceService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val personService: PersonService,
    private val helsenettProxyService: HelsenettProxyService,
) {
    private val logger = LoggerFactory.getLogger(SykmeldingPersistenceService::class.java)

    fun getSykmeldingById(sykmeldingId: String): SykmeldingDocument? {
        return sykmeldingRepository.findSykmeldingEntityBySykmeldingId(sykmeldingId)?.let {
            mapDatabaseEntityToSykmeldingResponse(it)
        }
    }

    fun saveSykmeldingPayload(
        payload: OpprettSykmeldingPayload,
        sykmeldingId: String,
        person: Person,
        sykmelder: HprSykmelder,
        ruleResult: RegulaResult,
    ): SykmeldingDocument? {
        logger.info("Lagrer sykmelding med id=${sykmeldingId}")
        val savedEntity =
            sykmeldingRepository.save(
                mapSykmeldingPayloadToDatabaseEntity(
                    payload = payload,
                    sykmeldingId = sykmeldingId,
                    pasient = person,
                    sykmelder = sykmelder,
                    ruleResult = ruleResult,
                ),
            )
        if (savedEntity.id == null) {
            logger.error("Lagring av sykmelding med id=$sykmeldingId feilet")
            return null
        }

        logger.info("Sykmelding with id=$sykmeldingId er lagret")

        return mapDatabaseEntityToSykmeldingResponse(savedEntity)
    }

    private fun mapSykmeldingPayloadToDatabaseEntity(
        payload: OpprettSykmeldingPayload,
        sykmeldingId: String,
        pasient: Person,
        sykmelder: HprSykmelder,
        ruleResult: RegulaResult,
    ): SykmeldingDb {
        logger.info("Mapper sykmelding payload til database entitet for sykmeldingId=$sykmeldingId")
        return SykmeldingDb(
            sykmeldingId = sykmeldingId,
            pasientIdent = payload.meta.pasientIdent,
            sykmelderHpr = payload.meta.sykmelderHpr,
            sykmelding =
                PersistedSykmeldingMapper.mapSykmeldingPayloadToPersistedSykmelding(
                        payload,
                        sykmeldingId,
                        pasient,
                        sykmelder,
                        ruleResult,
                    )
                    .toPGobject(),
            legekontorOrgnr = payload.meta.legekontorOrgnr,
            legekontorTlf = payload.meta.legekontorTlf,
        )
    }

    fun mapDatabaseEntityToSykmeldingResponse(dbObject: SykmeldingDb): SykmeldingDocument {
        return SykmeldingDocument(
            sykmeldingId = dbObject.sykmeldingId,
            meta =
                SykmeldingDocumentMeta(
                    pasientIdent = dbObject.pasientIdent,
                    sykmelderHpr = dbObject.sykmelderHpr,
                    legekontorOrgnr = dbObject.legekontorOrgnr,
                ),
            values =
                SykmeldingDocumentMapper.mapPersistedSykmeldingToSykmeldingDokument(
                    dbObject.sykmelding.fromPGobject(),
                ),
        )
    }

    fun mapSykmeldingRecordToSykmeldingDatabaseEntity(
        sykmeldingId: String,
        sykmeldingRecord: SykmeldingRecord,
        validertOk: Boolean,
        person: Person,
        sykmelder: HprSykmelder,
    ): SykmeldingDb {
        return SykmeldingDb(
            sykmeldingId = sykmeldingId,
            pasientIdent = sykmeldingRecord.sykmelding.pasient.fnr,
            sykmelderHpr = PersistedSykmeldingMapper.mapHprNummer(sykmeldingRecord),
            sykmelding =
                PersistedSykmeldingMapper.mapSykmeldingRecordToPersistedSykmelding(
                        sykmeldingRecord,
                        person,
                        sykmelder,
                    )
                    .toPGobject(),
            legekontorOrgnr = PersistedSykmeldingMapper.mapLegekontorOrgnr(sykmeldingRecord),
            legekontorTlf = PersistedSykmeldingMapper.mapLegekontorTlf(sykmeldingRecord),
            validertOk = validertOk,
        )
    }

    fun getSykmeldingerByIdent(ident: String): List<SykmeldingDocument> {
        return sykmeldingRepository.findAllByPasientIdent(ident).map {
            mapDatabaseEntityToSykmeldingResponse(it)
        }
    }

    fun updateSykmelding(sykmeldingId: String, sykmeldingRecord: SykmeldingRecord?) {
        if (sykmeldingRecord == null) {
            logger.info(
                "SykmeldingRecord is null, deleting sykmelding with id=$sykmeldingId from syk-inn-api database",
            )
            delete(sykmeldingId)
            return
        }

        val sykmeldingEntity = sykmeldingRepository.findSykmeldingEntityBySykmeldingId(sykmeldingId)
        val person =
            personService.getPersonByIdent(sykmeldingRecord.sykmelding.pasient.fnr).getOrThrow()

        val sykmelder =
            helsenettProxyService
                .getSykmelderByHpr(
                    PersistedSykmeldingMapper.mapHprNummer(sykmeldingRecord),
                    sykmeldingId,
                )
                .getOrThrow()

        val typeNotDigital = sykmeldingRecord.sykmelding.type != SykmeldingType.DIGITAL
        if (sykmeldingEntity == null && typeNotDigital) {
            logger.info("Sykmelding with id=$sykmeldingId is not found in DB, creating new entry")
            try {
                // TODO skal sjekke om den faktisk er avvist eller ikkje før en kan sette validert
                // ok.
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
                logger.info(
                    "Unable to map SykmeldingRecord to database entity for sykmeldingId=$sykmeldingId and will therefore be skipped and not saved",
                )
                logger.error(
                    "Failed to map SykmeldingRecord to SykmeldingDb for sykmeldingId=$sykmeldingId",
                    ex,
                )
                throw SykmeldingDBMappingException(
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

    private fun delete(sykmeldingId: String) {
        sykmeldingRepository.deleteBySykmeldingId(sykmeldingId)
        logger.info("Deleted sykmelding with id=$sykmeldingId")
    }
}
