package no.nav.tsm.syk_inn_api.sykmelding.scheduled

import no.nav.tsm.syk_inn_api.sykmelding.metrics.SykmeldingMetrics
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

const val DAYS_OLD_SYKMELDING = 365L

@Component
class ScheduledTasks(
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
    private val sykmeldingMetrics: SykmeldingMetrics,
) {
    private val logger = logger()
    private val lastScheduledDeletionTimestamp = AtomicLong(0)

    init {
        // Register gauge for last scheduled deletion timestamp
        sykmeldingMetrics.registerLastScheduledDeletionTimestamp(lastScheduledDeletionTimestamp)
    }

    @Scheduled(cron = "0 0 2 * * ?") // Runs every day at 2 AM
    @Transactional
    fun deleteOldSykmeldinger() {
        val startTime = Instant.now()
        logger.info("Starting task to delete old sykmeldinger older than $DAYS_OLD_SYKMELDING days")

        try {
            val deletedCount =
                sykmeldingPersistenceService.deleteSykmeldingerOlderThanDays(DAYS_OLD_SYKMELDING)

            sykmeldingMetrics.recordScheduledDeletionDuration(
                Duration.between(startTime, Instant.now()),
            )
            lastScheduledDeletionTimestamp.set(Instant.now().epochSecond)

            logger.info("Successfully deleted $deletedCount sykmeldinger")
        } catch (e: Exception) {
            sykmeldingMetrics.incrementScheduledDeletionFailure()
            logger.error("Failed to delete old sykmeldinger", e)
            throw e
        }
    }
}
