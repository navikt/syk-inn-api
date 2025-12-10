package no.nav.tsm.syk_inn_api.config

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.simpleclient.bridge.SimpleclientCollector
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!test")
class PrometheusBridgeConfig(prometheusMeterRegistry: PrometheusMeterRegistry) {
    init {
        val registry: PrometheusRegistry = prometheusMeterRegistry.prometheusRegistry

        // Register the bridge with Micrometer's Prometheus registry
        SimpleclientCollector.builder().register(registry)
    }
}
