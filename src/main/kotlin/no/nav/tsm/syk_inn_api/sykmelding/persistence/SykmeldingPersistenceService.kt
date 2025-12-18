package no.nav.tsm.syk_inn_api.sykmelding.persistence

import java.time.OffsetDateTime
import java.util.UUID
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface SykInnPersistence {
    fun saveNewSykmelding(sykmeldingDb: SykmeldingDb, sendTimestamp: OffsetDateTime?): SykmeldingDb

    fun getSykmeldingByIdempotencyKey(submitId: UUID): SykmeldingDb?

    fun getSykmeldingByPasientIdent(pasientIdent: String): List<SykmeldingDb>

    fun getBySykmeldingId(sykmeldingId: String): SykmeldingDb?
}

interface SykmeldingPersistence {
    fun saveSykmelding(sykmeldingDb: SykmeldingDb): SykmeldingDb

    fun deleteSykmelding(sykmeldingId: String)
}

@Service
@Transactional
class SykmeldingPersistenceService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val sykmeldingStatusRepository: SykmeldingStatusRepository
) : SykInnPersistence, SykmeldingPersistence {
    private val logger = logger()

    override fun getSykmeldingByIdempotencyKey(submitId: UUID): SykmeldingDb? {
        return sykmeldingRepository.getSykmeldingDbByIdempotencyKey(submitId)
    }

    override fun getSykmeldingByPasientIdent(pasientIdent: String): List<SykmeldingDb> {
        return sykmeldingRepository.findAllByPasientIdent(pasientIdent)
    }

    override fun getBySykmeldingId(sykmeldingId: String): SykmeldingDb? {
        return sykmeldingRepository.getSykmeldingDbBySykmeldingId(sykmeldingId)
    }

    override fun deleteSykmelding(sykmeldingId: String) {
        sykmeldingRepository.deleteBySykmeldingId(sykmeldingId)
        logger.info("Deleted sykmelding with id=$sykmeldingId")
    }

    override fun saveNewSykmelding(
        sykmeldingDb: SykmeldingDb,
        sendTimestamp: OffsetDateTime?
    ): SykmeldingDb {
        val savedEntity =
            sykmeldingRepository.save(
                sykmeldingDb,
            )
        sykmeldingStatusRepository.insert(
            UUID.fromString(sykmeldingDb.sykmeldingId),
            sykmeldingDb.mottatt,
            sendTimestamp ?: sykmeldingDb.mottatt,
            "MY (FHIR)"
        )
        return savedEntity
    }

    override fun saveSykmelding(sykmeldingDb: SykmeldingDb): SykmeldingDb {
        return sykmeldingRepository.save(sykmeldingDb)
    }
}
