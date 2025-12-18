package no.nav.tsm.syk_inn_api.sykmelding.scheduled

import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingRepository
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

const val DAYS_OLD_SYKMELDING = 365L

@Component
class ScheduledTasks(private val sykmeldingRepository: SykmeldingRepository) {
    private val logger = logger()

    @Scheduled(cron = "0 0 2 * * ?") // Runs every day at 2 AM
    @Transactional
    fun deleteOldSykmeldinger() {
        logger.info("Starting task to delete old sykmeldinger older than $DAYS_OLD_SYKMELDING days")

        val cutoffDate = java.time.LocalDate.now().minusDays(DAYS_OLD_SYKMELDING)
        val deletedCount = sykmeldingRepository.deleteSykmeldingerWithAktivitetOlderThan(cutoffDate)
        logger.info("Successfully deleted $deletedCount sykmeldinger")
    }
}
