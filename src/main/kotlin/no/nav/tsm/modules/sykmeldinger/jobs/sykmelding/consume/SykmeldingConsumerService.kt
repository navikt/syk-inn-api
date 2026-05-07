package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import arrow.core.getOrElse
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.core.Environment
import no.nav.tsm.core.RuntimeEnvironments
import no.nav.tsm.core.logger
import no.nav.tsm.core.utils.sykmeldingCutoffDate
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.poison.SykmeldingPoisonPillRepo
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord

class SykmeldingConsumerService(
    private val environment: Environment,
    private val consumer: SykmeldingConsumer,
    private val sykmeldingConsumerRepo: SykmeldingConsumerRepo,
    private val sykmeldingConsumerResourcesService: SykmeldingConsumerResourcesService,
    private val sykmeldingPoisonPillRepo: SykmeldingPoisonPillRepo,
) {
    private val logger = logger()

    suspend fun consume() =
        withContext(Dispatchers.IO) {
            consumer.subscribe()
            try {
                while (isActive) {
                    val records = consumer.poll()
                    for ((key, sykmelding) in records) {
                        handleRecord(key, sykmelding)
                    }
                }
            } catch (ex: Exception) {
                logger.error("Kafka consumer loop threw an exception", ex)
                throw ex
            } finally {
                consumer.unsubscribe()
            }
        }

    @WithSpan(kind = SpanKind.CONSUMER, inheritContext = false)
    private suspend fun handleRecord(
        @SpanAttribute("sykmelding.id") key: String,
        sykmelding: SykmeldingRecord?,
    ) {
        if (sykmelding == null) return deleteSykmelding(key)

        if (isOverRetentionPeriod(sykmelding)) {
            logger.debug("Skipping sykmelding over retention period $key")
            return
        }

        val withResources: RecordWithResources =
            sykmeldingConsumerResourcesService.getResourcesForSykmelding(sykmelding).getOrElse {
                resourceError ->
                if (
                    environment.runtime.env == RuntimeEnvironments.DEV &&
                        resourceError.skippableInDev
                ) {
                    logger.warn(
                        "Found skippable error in dev: ${resourceError.javaClass.simpleName} (${key}), ignoring!"
                    )
                    return
                } else {
                    error("Unrecoverable error! ${resourceError.javaClass.name} (${key})")
                }
            }

        try {
            val verifiedSykmelding = withResources.toVerifiedSykmelding()

            sykmeldingConsumerRepo.insert(verifiedSykmelding)
            logger.debug("Sykmelding inserted ${verifiedSykmelding.sykmeldingId}")
        } catch (ex: Exception) {
            val key = UUID.fromString(withResources.record.sykmelding.id)
            val poisoned = sykmeldingPoisonPillRepo.isPoisoned(key)
            if (poisoned != null) {
                logger.warn(
                    "Found poisoned sykmelding (on root) ${key}, reason ${poisoned.reason} at ${poisoned.created}"
                )

                return
            }

            throw ex
        }
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
        sykmelding.sykmelding.aktivitet.maxOf { it.tom } < environment.sykmeldingCutoffDate()
}
