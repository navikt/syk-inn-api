package no.nav.tsm.syk_inn_api.sykmelding.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import org.springframework.stereotype.Component

/**
 * Service Level Indicators (SLIs) and Service Level Objectives (SLOs) for sykmelding operations.
 *
 * ## Service Level Objectives (SLOs):
 * - **Availability**: ≥99.5% of requests succeed
 * - **Latency p95**: Create operations ≤2000ms, Verify operations ≤500ms
 * - **Error Rate**: ≤1% of all operations fail
 * - **Consumer Freshness**: Consumer lag ≤60s
 *
 * ## Service Level Agreement (SLA):
 * - **Uptime**: 99.0% per month
 * - Consequences: Operational review and incident analysis for violations
 */
@Component
class SykmeldingServiceLevelIndicators(private val registry: MeterRegistry) {

    companion object {
        // SLO thresholds
        const val SLO_AVAILABILITY_PERCENT = 99.5
        const val SLO_CREATE_LATENCY_P95_MS = 2000L
        const val SLO_VERIFY_LATENCY_P95_MS = 500L
        const val SLO_ERROR_RATE_PERCENT = 1.0
        const val SLO_CONSUMER_LAG_SECONDS = 60L

        // SLA threshold
        const val SLA_UPTIME_PERCENT = 99.0
    }

    private val totalRequests = AtomicLong(0)
    private val successfulRequests = AtomicLong(0)
    private val failedRequests = AtomicLong(0)

    private val lastConsumerProcessing = AtomicLong(Instant.now().epochSecond)

    init {
        // Register SLI gauges
        registry.gauge("sykmelding.sli.availability.percent", totalRequests) {
            calculateAvailability()
        }

        registry.gauge("sykmelding.sli.error.rate.percent", failedRequests) { calculateErrorRate() }

        registry.gauge("sykmelding.sli.consumer.lag.seconds", lastConsumerProcessing) {
            calculateConsumerLag()
        }

        // Register SLO violation counters
        registry.counter("sykmelding.slo.availability.violation")
        registry.counter("sykmelding.slo.latency.create.violation")
        registry.counter("sykmelding.slo.latency.verify.violation")
        registry.counter("sykmelding.slo.error.rate.violation")
        registry.counter("sykmelding.slo.consumer.lag.violation")
    }

    fun recordSuccessfulRequest(operation: String) {
        totalRequests.incrementAndGet()
        successfulRequests.incrementAndGet()

        registry
            .counter("sykmelding.sli.requests.total", listOf(Tag.of("operation", operation)))
            .increment()
        registry
            .counter("sykmelding.sli.requests.successful", listOf(Tag.of("operation", operation)))
            .increment()
    }

    fun recordFailedRequest(operation: String) {
        totalRequests.incrementAndGet()
        failedRequests.incrementAndGet()

        registry
            .counter("sykmelding.sli.requests.total", listOf(Tag.of("operation", operation)))
            .increment()
        registry
            .counter("sykmelding.sli.requests.failed", listOf(Tag.of("operation", operation)))
            .increment()

        checkAvailabilitySLO()
        checkErrorRateSLO()
    }

    fun checkLatencySLO(operation: String, duration: Duration) {
        val millis = duration.toMillis()
        val threshold =
            when (operation) {
                "create" -> SLO_CREATE_LATENCY_P95_MS
                "verify" -> SLO_VERIFY_LATENCY_P95_MS
                else -> return
            }

        if (millis > threshold) {
            registry.counter("sykmelding.slo.latency.$operation.violation").increment()
        }
    }

    fun updateConsumerProcessingTimestamp() {
        lastConsumerProcessing.set(Instant.now().epochSecond)
        checkConsumerLagSLO()
    }

    private fun calculateAvailability(): Double {
        val total = totalRequests.get()
        if (total == 0L) return 100.0

        val successful = successfulRequests.get()
        return (successful.toDouble() / total.toDouble()) * 100.0
    }

    private fun calculateErrorRate(): Double {
        val total = totalRequests.get()
        if (total == 0L) return 0.0

        val failed = failedRequests.get()
        return (failed.toDouble() / total.toDouble()) * 100.0
    }

    private fun calculateConsumerLag(): Double {
        val lastProcessing = lastConsumerProcessing.get()
        val now = Instant.now().epochSecond
        return (now - lastProcessing).toDouble()
    }

    private fun checkAvailabilitySLO() {
        val availability = calculateAvailability()
        if (availability < SLO_AVAILABILITY_PERCENT) {
            registry.counter("sykmelding.slo.availability.violation").increment()
        }
    }

    private fun checkErrorRateSLO() {
        val errorRate = calculateErrorRate()
        if (errorRate > SLO_ERROR_RATE_PERCENT) {
            registry.counter("sykmelding.slo.error.rate.violation").increment()
        }
    }

    private fun checkConsumerLagSLO() {
        val lag = calculateConsumerLag()
        if (lag > SLO_CONSUMER_LAG_SECONDS) {
            registry.counter("sykmelding.slo.consumer.lag.violation").increment()
        }
    }

    fun getLastConsumerProcessingAtomicLong(): AtomicLong = lastConsumerProcessing
}
