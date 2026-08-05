package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import arrow.core.getOrElse
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.util.*
import no.nav.tsm.core.Environment
import no.nav.tsm.core.utils.sykmeldingCutoffDate
import no.nav.tsm.ktor.logger
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.poison.SykmeldingPoisonPillRepo
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord

class SykmeldingConsumerService(
    private val environment: Environment,
    private val sykmeldingConsumerRepo: SykmeldingConsumerRepo,
    private val sykmeldingConsumerResourcesService: SykmeldingConsumerResourcesService,
    private val sykmeldingPoisonPillRepo: SykmeldingPoisonPillRepo,
) {
    private val logger = logger()

    @WithSpan(kind = SpanKind.CONSUMER, inheritContext = false)
    suspend fun handleRecord(sykmelding: SykmeldingRecord) {
        val span = Span.current()
        val key = sykmelding.sykmelding.id
        span.setAttribute("sykmelding.id", key)

        if (isOverRetentionPeriod(sykmelding)) {
            logger.debug("Skipping sykmelding over retention period $key")
            return
        }

        val withResources: RecordWithResources =
            sykmeldingConsumerResourcesService.getResourcesForSykmelding(sykmelding).getOrElse {
                resourceError ->
                handleError(resourceError, key)
                return
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

    suspend fun handleTombstone(key: String) {
        deleteSykmelding(key)
    }

    private fun handleError(resourceError: RecordResourceErrors, key: String) {
        if (environment.runtime.env == RuntimeCluster.DEV && resourceError.skippableInDev) {
            logger.warn(
                "Found skippable error in dev: ${resourceError.javaClass.simpleName} (${key}), ignoring!"
            )
            return
        } else {
            error("Unrecoverable error! ${resourceError.javaClass.name} (${key})")
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
