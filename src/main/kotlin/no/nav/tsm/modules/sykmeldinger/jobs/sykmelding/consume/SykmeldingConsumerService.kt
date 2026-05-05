package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.core.Environment
import no.nav.tsm.core.logger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord

class SykmeldingConsumerService(
    private val environment: Environment,
    private val consumer: SykmeldingConsumer,
    private val sykmeldingConsumerRepo: SykmeldingConsumerRepo,
    private val sykmeldingConsumerResourcesService: SykmeldingConsumerResourcesService,
) {
    private val logger = logger()

    suspend fun consume() =
        withContext(Dispatchers.IO) {
            consumer.subscribe()
            try {
                while (isActive) {
                    val records = consumer.poll()
                    for ((key, sykmelding) in records) {
                        handleSykmelding(key, sykmelding)
                    }
                }
            } catch (ex: Exception) {
                logger.error("Kafka consumer loop threw an exception", ex)
                throw ex
            } finally {
                consumer.unsubscribe()
            }
        }

    @WithSpan
    private suspend fun handleSykmelding(key: String, sykmelding: SykmeldingRecord?) {
        if (sykmelding == null) return deleteSykmelding(key)

        if (isOverRetentionPeriod(sykmelding)) {
            logger.debug("Skipping sykmelding over retention period $key")
            return
        }

        val withResources = sykmeldingConsumerResourcesService.getResourcesForSykmelding(sykmelding)
        val verifiedSykmelding = withResources.toVerifiedSykmelding()

        sykmeldingConsumerRepo.insert(verifiedSykmelding)
        logger.debug("Sykmelding inserted ${verifiedSykmelding.sykmeldingId}")
    }

    @WithSpan
    private suspend fun deleteSykmelding(key: String) {
        logger.debug("Tombstone for sykmelding $key, deleting")
        try {
            val sykmeldingId = UUID.fromString(key)
            val deleted = sykmeldingConsumerRepo.delete(sykmeldingId)
            logger.debug(
                "${if (deleted >= 1) "Deleted" else "Sykmelding did not exist"} sykmeldingId: $sykmeldingId"
            )
        } catch (ex: Exception) {
            logger.error("Could not delete sykmelding", ex)
        }
    }

    private fun isOverRetentionPeriod(sykmelding: SykmeldingRecord): Boolean =
        sykmelding.sykmelding.aktivitet.maxOf { it.tom } <
            (LocalDate.now().minusDays(environment.sykmeldingConfig.retention.inWholeDays))
}
