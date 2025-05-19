package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.syk_inn_api.model.sykmelding.SavedSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingEntity
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingRecord
import no.nav.tsm.syk_inn_api.repository.SykmeldingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SykmeldingPersistenceService(private val sykmeldingRepository: SykmeldingRepository) {
    private val logger = LoggerFactory.getLogger(SykmeldingPersistenceService::class.java)

    fun getSykmeldingById(sykmeldingId: String): SykmeldingEntity? {
        return sykmeldingRepository.findSykmeldingEntityBySykmeldingId(sykmeldingId)
    }

    fun save(payload: SykmeldingPayload, sykmeldingId: String): SykmeldingEntity {
        logger.info("Lagrer sykmelding med id=${sykmeldingId}")
        return sykmeldingRepository.save(
            mapToEntity(
                payload = payload,
                sykmeldingId = sykmeldingId,
            ),
        )
    }

    private fun mapToEntity(payload: SykmeldingPayload, sykmeldingId: String): SykmeldingEntity {
        logger.info("Mapping sykmelding til entity")
        return SykmeldingEntity(
            sykmeldingId = sykmeldingId,
            pasientFnr = payload.pasientFnr,
            sykmelderHpr = payload.sykmelderHpr,
            sykmelding = payload.sykmelding,
            legekontorOrgnr = payload.legekontorOrgnr,
        )
    }

    private fun mapToSavedSykmelding(sykmelding: SykmeldingEntity): SavedSykmelding {
        return SavedSykmelding(
            sykmeldingId = sykmelding.sykmeldingId,
            pasientFnr = sykmelding.pasientFnr,
            sykmelderHpr = sykmelding.sykmelderHpr,
            sykmelding = sykmelding.sykmelding,
            legekontorOrgnr = sykmelding.legekontorOrgnr,
        )
    }

    fun getSykmeldingerByIdent(ident: String): List<SykmeldingEntity> {
        return sykmeldingRepository.findSykmeldingEntitiesByPasientFnr(ident)
    }

    fun updateSykmelding(key: String?, value: SykmeldingRecord?) {
        TODO("Not yet implemented")
    }
}
