package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.syk_inn_api.model.sykmelding.SavedSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingDb
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingMapper
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.model.sykmelding.fromPGobject
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingRecord
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingType
import no.nav.tsm.syk_inn_api.model.sykmelding.toPGobject
import no.nav.tsm.syk_inn_api.repository.SykmeldingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SykmeldingPersistenceService(
    private val sykmeldingRepository: SykmeldingRepository,
) {
    private val logger = LoggerFactory.getLogger(SykmeldingPersistenceService::class.java)

    fun getSykmeldingById(sykmeldingId: String): SykmeldingDb? {
        return sykmeldingRepository.findSykmeldingEntityBySykmeldingId(sykmeldingId)
    }

    fun save(payload: SykmeldingPayload, sykmeldingId: String): SykmeldingDb {
        logger.info("Lagrer sykmelding med id=${sykmeldingId}")
        return sykmeldingRepository.save(
            mapToEntity(
                payload = payload,
                sykmeldingId = sykmeldingId,
            ),
        )
    }

    private fun mapToEntity(payload: SykmeldingPayload, sykmeldingId: String): SykmeldingDb {
        logger.info("Mapping sykmelding til entity")
        return SykmeldingDb(
            sykmeldingId = sykmeldingId,
            pasientFnr = payload.pasientFnr,
            sykmelderHpr = payload.sykmelderHpr,
            sykmelding = payload.sykmelding.toPGobject(),
            legekontorOrgnr = payload.legekontorOrgnr,
        )
    }

    private fun mapToSavedSykmelding(sykmelding: SykmeldingDb): SavedSykmelding {
        return SavedSykmelding(
            sykmeldingId = sykmelding.sykmeldingId,
            pasientFnr = sykmelding.pasientFnr,
            sykmelderHpr = sykmelding.sykmelderHpr,
            sykmelding = sykmelding.sykmelding.fromPGobject(),
            legekontorOrgnr = sykmelding.legekontorOrgnr,
        )
    }

    fun getSykmeldingerByIdent(ident: String): List<SykmeldingDb> {
        return sykmeldingRepository.findAllByPasientFnr(ident)
    }

    fun updateSykmelding(sykmeldingId: String, sykmeldingRecord: SykmeldingRecord?) {
        logger.info("Inside the method to update sykmelding with id=$sykmeldingId")
        if (sykmeldingRecord == null) {
            logger.info(
                "SykmeldingRecord is null, deleting sykmelding with id=$sykmeldingId from syk-inn-api database"
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
        logger.info(
            "is $sykmeldingId equal to ${sykmeldingRecord.sykmelding.id} ?"
        ) // TODO delete this after testing

        if (
            sykmeldingEntity == null && sykmeldingRecord.sykmelding.type != SykmeldingType.DIGITAL
        ) {
            logger.info("Sykmelding with id=$sykmeldingId is not found in DB, creating new entry")
            sykmeldingRepository.save(
                SykmeldingMapper.mapToSykmeldingDb(sykmeldingId, sykmeldingRecord, true),
            )
            logger.debug("Saved sykmelding with id=${sykmeldingRecord.sykmelding.id}")
        }

        if (sykmeldingRecord.sykmelding.type == SykmeldingType.DIGITAL) {
            val updatedEntity = sykmeldingEntity?.copy(validertOk = true)
            logger.info("Updating sykmelding with id=${sykmeldingRecord.sykmelding.id}")
            sykmeldingRepository.save(
                updatedEntity
                    ?: SykmeldingMapper.mapToSykmeldingDb(sykmeldingId, sykmeldingRecord, true),
            )
            logger.info("Updated sykmelding with id=${sykmeldingRecord.sykmelding.id}")
        }
    }

    private fun delete(sykmeldingId: String) {
        sykmeldingRepository.deleteBySykmeldingId(sykmeldingId)
        logger.info("Deleted sykmelding with id=$sykmeldingId")
    }
}
