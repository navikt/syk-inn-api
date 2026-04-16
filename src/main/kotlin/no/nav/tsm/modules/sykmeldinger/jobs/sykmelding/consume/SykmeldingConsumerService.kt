package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import java.util.UUID
import kotlin.time.toJavaDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.core.Environment
import no.nav.tsm.core.SykmeldingConfig
import no.nav.tsm.core.logger
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding

class SykmeldingConsumerService(
    environment: Environment,
    private val sykmeldingConsumerRepo: SykmeldingConsumerRepo,
    private val consumer: SykmeldingConsumer,
    private val sykmeldingConfig: SykmeldingConfig = environment.sykmeldingConfig,
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
    private suspend fun handleSykmelding(key: String, sykmelding: VerifiedSykInnSykmelding?) {
        when {
            sykmelding == null -> deleteSykmelding(key)
            isOverRetentionPeriod(sykmelding) -> {
                logger.debug("Skipping sykmelding over retention period $key")
            }
            else -> {
                sykmeldingConsumerRepo.insert(sykmelding)
                logger.debug("Sykmelding inserted ${sykmelding.sykmeldingId}")
            }
        }
    }

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

    private fun isOverRetentionPeriod(sykmelding: VerifiedSykInnSykmelding): Boolean =
        sykmelding.values.aktivitet.maxOf { it.tom } >
            (LocalDate.now() - sykmeldingConfig.retention.toJavaDuration())
}
