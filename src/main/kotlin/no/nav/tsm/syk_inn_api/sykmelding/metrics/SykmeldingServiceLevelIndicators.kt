package no.nav.tsm.syk_inn_api.sykmelding.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import java.time.Duration
import org.springframework.stereotype.Component

/**
 * Service Level Indicators (SLIs) for sykmelding operations.
 *
 * All metrics are counter-based to work correctly in multi-pod Kubernetes deployments.
 * Calculate SLIs in Grafana/PromQL, e.g.:
 * - Availability: sum(rate(sykmelding_sli_requests_successful_total[15m])) / sum(rate(sykmelding_sli_requests_total[15m])) * 100
 * - Error Rate: sum(rate(sykmelding_sli_requests_failed_total[15m])) / sum(rate(sykmelding_sli_requests_total[15m])) * 100
 *
 * ## Service Level Objectives (SLOs):
 * - **Availability**: ≥99.5% of requests succeed
 * - **Latency p95**: Create operations ≤2000ms, Verify operations ≤500ms
 * - **Error Rate**: ≤1% of all operations fail
 */
@Component
class SykmeldingServiceLevelIndicators(private val registry: MeterRegistry) {

    companion object {
        // SLO thresholds
        const val SLO_CREATE_LATENCY_P95_MS = 2000L
        const val SLO_VERIFY_LATENCY_P95_MS = 500L
    }

    fun recordSuccessfulRequest(operation: String) {
        registry
            .counter("sykmelding.sli.requests.total", listOf(Tag.of("operation", operation)))
            .increment()
        registry
            .counter("sykmelding.sli.requests.successful", listOf(Tag.of("operation", operation)))
            .increment()
    }

    fun recordFailedRequest(operation: String) {
        registry
            .counter("sykmelding.sli.requests.total", listOf(Tag.of("operation", operation)))
            .increment()
        registry
            .counter("sykmelding.sli.requests.failed", listOf(Tag.of("operation", operation)))
            .increment()
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
}
