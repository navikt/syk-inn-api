package no.nav.tsm.syk_inn_api.sykmelding.metrics

import no.nav.tsm.syk_inn_api.sykmelding.errors.ErrorRepository
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingRepository
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Scheduled collector that queries the database periodically to update gauge metrics.
 * Runs every 60 seconds to keep metrics fresh.
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
        val allSykmeldinger = sykmeldingRepository.findAll().toList()

        activeSykmeldingerCount.set(allSykmeldinger.size.toLong())

        val validatedOk = allSykmeldinger.count { it.validertOk }
        validatedOkCount.set(validatedOk.toLong())
        validatedNotOkCount.set((allSykmeldinger.size - validatedOk).toLong())

        // Calculate oldest sykmelding age
        if (allSykmeldinger.isNotEmpty()) {
            val oldestFom = allSykmeldinger.minOfOrNull { it.fom }
            if (oldestFom != null) {
                val ageDays = ChronoUnit.DAYS.between(oldestFom, LocalDate.now())
                oldestSykmeldingAgeDays.set(ageDays)
            }
        } else {
            oldestSykmeldingAgeDays.set(0)
        }
    }

    private fun collectErrorMetrics() {
        val allErrors = errorRepository.findAll().toList()
        errorQueueSize.set(allErrors.size.toLong())

        // Calculate oldest error age in hours (assuming errors have timestamp if available)
        // For now, we'll set to 0 as KafkaProcessingError doesn't have a timestamp field
        // This can be enhanced if timestamp is added to the entity
        oldestErrorAgeHours.set(0)
    }
}

