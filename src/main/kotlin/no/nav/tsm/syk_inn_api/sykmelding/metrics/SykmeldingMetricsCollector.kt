package no.nav.tsm.syk_inn_api.sykmelding.metrics

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicLong
import no.nav.tsm.syk_inn_api.sykmelding.errors.ErrorRepository
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingRepository
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduled collector that queries the database periodically to update gauge metrics.
 * Runs every 60 seconds to keep metrics fresh.
 *
 * These gauges are useful for operational dashboards to monitor "normal drift":
 * - Are we persisting and processing sykmeldinger at expected rates?
 * - Is the error queue growing?
 * - How old is the oldest sykmelding in the system?
 */
@Component
class SykmeldingMetricsCollector(
    private val sykmeldingRepository: SykmeldingRepository,
    private val errorRepository: ErrorRepository,
    sykmeldingMetrics: SykmeldingMetrics,
) {
    private val logger = logger()

    private val activeSykmeldingerCount = AtomicLong(0)
    private val validatedOkCount = AtomicLong(0)
    private val validatedNotOkCount = AtomicLong(0)
    private val errorQueueSize = AtomicLong(0)
    private val oldestSykmeldingAgeDays = AtomicLong(0)
    private val oldestErrorAgeHours = AtomicLong(0)

    init {
        // Register gauges with the metrics component
        sykmeldingMetrics.registerActiveSykmeldingerGauge(activeSykmeldingerCount)
        sykmeldingMetrics.registerValidatedOkGauge(validatedOkCount)
        sykmeldingMetrics.registerValidatedNotOkGauge(validatedNotOkCount)
        sykmeldingMetrics.registerErrorQueueSizeGauge(errorQueueSize)
        sykmeldingMetrics.registerOldestSykmeldingAgeGauge(oldestSykmeldingAgeDays)
        sykmeldingMetrics.registerOldestErrorAgeGauge(oldestErrorAgeHours)
    }

    @Scheduled(fixedRate = 60_000) // Run every 60 seconds
    fun collectMetrics() {
        try {
            collectSykmeldingMetrics()
            collectErrorMetrics()
        } catch (e: Exception) {
            logger.error("Failed to collect metrics", e)
        }
    }

    private fun collectSykmeldingMetrics() {
        // Use efficient count queries instead of loading all entities
        activeSykmeldingerCount.set(sykmeldingRepository.countAll())
        validatedOkCount.set(sykmeldingRepository.countValidertOk())
        validatedNotOkCount.set(sykmeldingRepository.countValidertNotOk())

        // Calculate oldest sykmelding age
        val oldestFom = sykmeldingRepository.findMinFom()
        if (oldestFom != null) {
            val ageDays = ChronoUnit.DAYS.between(oldestFom, LocalDate.now())
            oldestSykmeldingAgeDays.set(ageDays)
        } else {
            oldestSykmeldingAgeDays.set(0)
        }
    }

    private fun collectErrorMetrics() {
        errorQueueSize.set(errorRepository.count())
        // Error age tracking can be added if timestamp is added to KafkaProcessingError entity
        oldestErrorAgeHours.set(0)
    }
}
