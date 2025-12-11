package no.nav.tsm.syk_inn_api.sykmelding.persistence

import java.util.UUID
import no.nav.tsm.syk_inn_api.sykmelding.scheduled.SykmeldingStatusRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SykmeldingPersistenceService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val sykmeldingStatusRepository: SykmeldingStatusRepository
) {

    @Transactional
    fun saveSykmelding(
        sykmeldingDb: SykmeldingDb,
    ): SykmeldingDb {
        val savedEntity =
            sykmeldingRepository.save(
                sykmeldingDb,
            )
        sykmeldingStatusRepository.insert(
            UUID.fromString(sykmeldingDb.sykmeldingId),
            sykmeldingDb.mottatt
        )
        return savedEntity
    }
}
