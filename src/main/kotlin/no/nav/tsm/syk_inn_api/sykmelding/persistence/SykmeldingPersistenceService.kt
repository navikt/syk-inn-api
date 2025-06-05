package no.nav.tsm.syk_inn_api.sykmelding.persistence

import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.exception.SykmeldingDBMappingException
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecord
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingType
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingResponse
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingResponseMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SykmeldingPersistenceService(
    private val sykmeldingRepository: SykmeldingRepository,
) {
    private val logger = LoggerFactory.getLogger(SykmeldingPersistenceService::class.java)

    fun getSykmeldingById(sykmeldingId: String): SykmeldingResponse? {
        return sykmeldingRepository.findSykmeldingEntityBySykmeldingId(sykmeldingId)?.let {
            mapDatabaseEntityToSykmeldingResponse(it)
        }
    }

    fun saveSykmeldingPayload(
        payload: SykmeldingPayload,
        sykmeldingId: String,
        person: Person,
        sykmelder: HprSykmelder,
        ruleResult: RegulaResult,
        ): SykmeldingResponse? {
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
        payload: SykmeldingPayload,
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

    fun mapDatabaseEntityToSykmeldingResponse(dbObject: SykmeldingDb): SykmeldingResponse {
        return SykmeldingResponse(
            sykmeldingId = dbObject.sykmeldingId,
            pasientFnr = dbObject.pasientIdent,
            sykmelderHpr = dbObject.sykmelderHpr,
            sykmelding =
                SykmeldingResponseMapper.mapPersistedSykmeldingToExistingSykmelding(
                    dbObject.sykmelding.fromPGobject(),
                ),
            legekontorOrgnr = dbObject.legekontorOrgnr,
        )
    }

    fun mapSykmeldingRecordToSykmeldingDatabaseEntity(
        sykmeldingId: String,
        sykmeldingRecord: SykmeldingRecord,
        validertOk: Boolean
    ): SykmeldingDb {
        return SykmeldingDb(
            sykmeldingId = sykmeldingId,
            pasientIdent = sykmeldingRecord.sykmelding.pasient.fnr,
            sykmelderHpr = PersistedSykmeldingMapper.mapHprNummer(sykmeldingRecord),
            sykmelding =
                PersistedSykmeldingMapper.mapSykmeldingRecordToPersistedSykmelding(sykmeldingRecord)
                    .toPGobject(),
            legekontorOrgnr = PersistedSykmeldingMapper.mapLegekontorOrgnr(sykmeldingRecord),
            validertOk = validertOk,
        )
    }

    fun getSykmeldingerByIdent(ident: String): List<SykmeldingResponse> {
        return sykmeldingRepository.findAllByPasientFnr(ident).map {
            mapDatabaseEntityToSykmeldingResponse(it)
        }
    }

    fun updateSykmelding(sykmeldingId: String, sykmeldingRecord: SykmeldingRecord?) {
        logger.info("Inside the method to update sykmelding with id=$sykmeldingId")
        if (sykmeldingRecord == null) {
            logger.info(
                "SykmeldingRecord is null, deleting sykmelding with id=$sykmeldingId from syk-inn-api database",
            )
            delete(sykmeldingId)
            return
        }

        logger.info("getting sykmelding with id $sykmeldingId from DB")
        val sykmeldingEntity = sykmeldingRepository.findSykmeldingEntityBySykmeldingId(sykmeldingId)
        if (sykmeldingEntity != null) {
            logger.info("Sykmelding with id=$sykmeldingId found in DB")
        } else {
            logger.info("Sykmelding with id=$sykmeldingId not found in DB")
        }

        if (
            sykmeldingEntity == null && sykmeldingRecord.sykmelding.type != SykmeldingType.DIGITAL
        ) {
            logger.info("Sykmelding with id=$sykmeldingId is not found in DB, creating new entry")
            try {
                //TODO skal sjekke om den faktisk er avvist eller ikkje før en kan sette validert ok.
                val entity =
                    mapSykmeldingRecordToSykmeldingDatabaseEntity(
                        sykmeldingId,
                        sykmeldingRecord,
                        true
                    )
                sykmeldingRepository.save(entity)
                logger.debug("Saved new sykmelding with id=${sykmeldingRecord.sykmelding.id}")
            } catch (ex: Exception) {
                logger.info(
                    "Unable to map SykmeldingRecord to database entity for sykmeldingId=$sykmeldingId and will therefore be skipped and not saved"
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
                        sykmeldingId,
                        sykmeldingRecord,
                        true,
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
