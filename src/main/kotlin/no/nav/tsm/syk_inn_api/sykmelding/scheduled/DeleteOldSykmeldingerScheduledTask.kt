package no.nav.tsm.syk_inn_api.sykmelding.scheduled

import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

const val DAYS_OLD_SYKMELDING = 365L

@Component
class ScheduledTasks(private val sykmeldingPersistenceService: SykmeldingPersistenceService) {
    private val logger = logger()

    @Scheduled(cron = "0 0 2 * * ?") // Runs every day at 2 AM
    fun deleteOldSykmeldinger() {
        logger.info("Starting task to delete old sykmeldinger older than $DAYS_OLD_SYKMELDING days")

        val deletedCount =
            sykmeldingPersistenceService.deleteSykmeldingerOlderThanDays(DAYS_OLD_SYKMELDING)

        logger.info("Successfully deleted $deletedCount sykmeldinger")
    }
}
