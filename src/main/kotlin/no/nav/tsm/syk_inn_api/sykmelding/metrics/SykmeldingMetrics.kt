package no.nav.tsm.syk_inn_api.sykmelding.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import org.springframework.stereotype.Component

@Component
class SykmeldingMetrics(private val registry: MeterRegistry) {

    // Counters for sykmelding operations
    fun incrementSykmeldingCreated(
        source: String,
        diagnoseSystem: DiagnoseSystem,
        validationResult: String,
        aktivitetType: String,
        yrkesskade: Boolean,
        svangerskapsrelatert: Boolean,
        tilbakedateringPresent: Boolean,
    ) {
        Counter.builder("sykmelding.created")
            .tag("source", source)
            .tag("diagnose_system", diagnoseSystem.name)
            .tag("validation_result", validationResult)
            .tag("aktivitet_type", aktivitetType)
            .tag("yrkesskade", yrkesskade.toString())
            .tag("svangerskapsrelatert", svangerskapsrelatert.toString())
            .tag("tilbakedatering_present", tilbakedateringPresent.toString())
            .description("Total number of sykmeldinger created")
            .register(registry)
            .increment()
    }

    fun incrementSykmeldingCreationFailed(errorType: String, source: String) {
        Counter.builder("sykmelding.creation.failed")
            .tag("error_type", errorType)
            .tag("source", source)
            .description("Total number of failed sykmelding creations")
            .register(registry)
            .increment()
    }

    fun incrementSykmeldingVerified(source: String, regulaStatus: String) {
        Counter.builder("sykmelding.verified")
            .tag("source", source)
            .tag("regula_status", regulaStatus)
            .description("Total number of sykmeldinger verified")
            .register(registry)
            .increment()
    }

    fun incrementSykmeldingVerificationFailed(errorType: String, source: String) {
        Counter.builder("sykmelding.verification.failed")
            .tag("error_type", errorType)
            .tag("source", source)
            .description("Total number of failed sykmelding verifications")
            .register(registry)
            .increment()
    }

    fun incrementSykmeldingUpdated(sykmeldingType: String) {
        Counter.builder("sykmelding.updated")
            .tag("sykmelding_type", sykmeldingType)
            .description("Total number of sykmeldinger updated from Kafka")
            .register(registry)
            .increment()
    }

    fun incrementSykmeldingDeleted() {
        Counter.builder("sykmelding.deleted")
            .description("Total number of sykmeldinger deleted")
            .register(registry)
            .increment()
    }

    // Timers for operation duration
    fun recordCreateDuration(duration: Duration, source: String) {
        Timer.builder("sykmelding.create.duration")
            .tag("source", source)
            .description("Time taken to create a sykmelding")
            .register(registry)
            .record(duration)
    }

    fun recordVerifyDuration(duration: Duration, source: String) {
        Timer.builder("sykmelding.verify.duration")
            .tag("source", source)
            .description("Time taken to verify a sykmelding")
            .register(registry)
            .record(duration)
    }

    fun recordRuleValidationDuration(duration: Duration) {
        Timer.builder("sykmelding.rule.validation.duration")
            .description("Time taken to validate rules")
            .register(registry)
            .record(duration)
    }

    // Database operation metrics
    fun incrementDatabaseSave() {
        Counter.builder("sykmelding.database.save")
            .description("Total number of database save operations")
            .register(registry)
            .increment()
    }

    fun recordDatabaseSaveDuration(duration: Duration) {
        Timer.builder("sykmelding.database.save.duration")
            .description("Time taken to save to database")
            .register(registry)
            .record(duration)
    }

    fun recordDatabaseQueryDuration(duration: Duration, operation: String) {
        Timer.builder("sykmelding.database.query.duration")
            .tag("operation", operation)
            .description("Time taken for database queries")
            .register(registry)
            .record(duration)
    }

    // Kafka consumer metrics
    fun incrementKafkaMessageConsumed() {
        Counter.builder("sykmelding.kafka.message.consumed")
            .description("Total number of Kafka messages consumed")
            .register(registry)
            .increment()
    }

    fun incrementKafkaTombstoneProcessed() {
        Counter.builder("sykmelding.kafka.tombstone.processed")
            .description("Total number of tombstone messages processed")
            .register(registry)
            .increment()
    }

    fun incrementKafkaProcessingError(exceptionType: String) {
        Counter.builder("sykmelding.kafka.processing.error")
            .tag("exception_type", exceptionType)
            .description("Total number of Kafka processing errors")
            .register(registry)
            .increment()
    }

    fun incrementKafkaPoisonPill() {
        Counter.builder("sykmelding.kafka.poison.pill")
            .description("Total number of poison pills encountered")
            .register(registry)
            .increment()
    }

    fun recordKafkaProcessingDuration(duration: Duration) {
        Timer.builder("sykmelding.kafka.processing.duration")
            .description("Time taken to process a Kafka message")
            .register(registry)
            .record(duration)
    }

    // Kafka producer metrics
    fun incrementKafkaMessageSent(source: String) {
        Counter.builder("sykmelding.kafka.message.sent")
            .tag("source", source)
            .description("Total number of Kafka messages sent")
            .register(registry)
            .increment()
    }

    fun incrementKafkaMessageSendFailed(source: String) {
        Counter.builder("sykmelding.kafka.message.send.failed")
            .tag("source", source)
            .description("Total number of failed Kafka message sends")
            .register(registry)
            .increment()
    }

    fun recordKafkaSendDuration(duration: Duration, source: String) {
        Timer.builder("sykmelding.kafka.send.duration")
            .tag("source", source)
            .description("Time taken to send a Kafka message")
            .register(registry)
            .record(duration)
    }

    // HTTP API metrics
    fun incrementHttpRequest(endpoint: String, method: String, statusCode: Int) {
        Counter.builder("sykmelding.http.request")
            .tag("endpoint", endpoint)
            .tag("method", method)
            .tag("status_code", statusCode.toString())
            .description("Total number of HTTP requests")
            .register(registry)
            .increment()
    }

    fun recordHttpRequestDuration(duration: Duration, endpoint: String, method: String) {
        Timer.builder("sykmelding.http.request.duration")
            .tag("endpoint", endpoint)
            .tag("method", method)
            .description("Time taken to process HTTP requests")
            .register(registry)
            .record(duration)
    }

    fun incrementAccessControlDenied(endpoint: String) {
        Counter.builder("sykmelding.access.control.denied")
            .tag("endpoint", endpoint)
            .description("Total number of access control denials")
            .register(registry)
            .increment()
    }

    // Scheduled task metrics
    fun incrementScheduledDeletion(count: Int) {
        Counter.builder("sykmelding.scheduled.deletion")
            .description("Total number of sykmeldinger deleted by scheduled task")
            .register(registry)
            .increment(count.toDouble())
    }

    fun recordScheduledDeletionDuration(duration: Duration) {
        Timer.builder("sykmelding.scheduled.deletion.duration")
            .description("Time taken for scheduled deletion task")
            .register(registry)
            .record(duration)
    }

    fun incrementScheduledDeletionFailure() {
        Counter.builder("sykmelding.scheduled.deletion.failure")
            .description("Total number of scheduled deletion failures")
            .register(registry)
            .increment()
    }

    // Error handler metrics
    fun incrementConsumerErrorHandled(exceptionType: String) {
        Counter.builder("sykmelding.consumer.error.handled")
            .tag("exception_type", exceptionType)
            .description("Total number of consumer errors handled")
            .register(registry)
            .increment()
    }

    fun incrementConsumerRetryAttempt() {
        Counter.builder("sykmelding.consumer.retry.attempt")
            .description("Total number of consumer retry attempts")
            .register(registry)
            .increment()
    }

    // Gauges (managed by MetricsCollector)
    fun registerActiveSykmeldingerGauge(valueSupplier: AtomicLong) {
        registry.gauge("sykmelding.active.count", valueSupplier)
    }

    fun registerValidatedOkGauge(valueSupplier: AtomicLong) {
        registry.gauge("sykmelding.validated.ok.count", valueSupplier)
    }

    fun registerValidatedNotOkGauge(valueSupplier: AtomicLong) {
        registry.gauge("sykmelding.validated.not.ok.count", valueSupplier)
    }

    fun registerErrorQueueSizeGauge(valueSupplier: AtomicLong) {
        registry.gauge("sykmelding.error.queue.size", valueSupplier)
    }

    fun registerOldestSykmeldingAgeGauge(valueSupplier: AtomicLong) {
        registry.gauge("sykmelding.oldest.age.days", valueSupplier)
    }

    fun registerOldestErrorAgeGauge(valueSupplier: AtomicLong) {
        registry.gauge("sykmelding.error.oldest.age.hours", valueSupplier)
    }

    fun registerLastConsumerProcessingTimestamp(atomicLong: AtomicLong) {
        registry.gauge("sykmelding.consumer.last.processing.timestamp", atomicLong)
    }

    fun registerLastScheduledDeletionTimestamp(atomicLong: AtomicLong) {
        registry.gauge("sykmelding.scheduled.deletion.last.run.timestamp", atomicLong)
    }

    // Histogram for sykmelding duration
    fun recordSykmeldingDuration(days: Long) {
        registry.summary("sykmelding.duration.days").record(days.toDouble())
    }
}
