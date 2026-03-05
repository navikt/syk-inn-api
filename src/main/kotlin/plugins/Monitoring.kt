package plugins

import dev.hayden.KHealth
import io.ktor.http.HttpHeaders
import io.ktor.server.application.*
import io.ktor.server.engine.ShutDownUrl
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    configureMicrometer()

    install(KHealth) {
        healthCheckPath = "/internal/health/alive"
        readyCheckPath = "/internal/health/ready"
    }
    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = "/internal/shutdown"
        exitCodeSupplier = { 0 }
    }
    install(CallId) {
        retrieveFromHeader("Traceparent")
        retrieveFromHeader(HttpHeaders.XRequestId)
    }
    install(CallLogging) {
        level = Level.DEBUG
        callIdMdc("call-id")
    }
}

private fun Application.configureMicrometer() {
    val appRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) { registry = appRegistry }
    routing { get("/internal/metrics") { call.respond(appRegistry.scrape()) } }
}
