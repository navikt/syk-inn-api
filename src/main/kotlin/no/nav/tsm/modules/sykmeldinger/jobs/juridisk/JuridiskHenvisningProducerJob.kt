package no.nav.tsm.modules.sykmeldinger.jobs.juridisk

import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.core.Environment
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.core.logger
import no.nav.tsm.modules.admin.service.JobName
import no.nav.tsm.regulus.regula.toJuridiskVurdering

const val JURIDISK_HENVISNING_EVENT_NAME = "subsumsjon"
const val JURIDISK_HENVISNING_VERSION = "1.0.0"
const val JURIDISK_HENVISNING_KILDE = "syk-inn-api"

class JuridiskHenvisningProducerJob(
    private val juridiskHenvisningProducer: JuridiskHenvisningProducer,
    private val juridiskHenvisningJobRepo: JuridiskHenvisningJobRepo,
    private val environment: Environment,
    applicationScope: CoroutineScope,
) : Job(JobName.JURIDISK_PRODUCER, applicationScope) {
    private val logger = logger()

    private val batchDelay = environment.jobs.juridiskProducer.delay
    private val hungJuridisk = environment.jobs.juridiskProducer.hungTimeout

    override suspend fun runJob() =
        withContext(Dispatchers.IO) {
            while (isActive) {
                delay(batchDelay)

                logger.debug("Running juridisk henvisning producer job (${batchDelay}ms)")
                handleJuridiskHenvisningBatch()
            }
        }

    @WithSpan(inheritContext = false)
    private suspend fun handleJuridiskHenvisningBatch() {
        val span = Span.current()

        juridiskHenvisningJobRepo
            .resetHangingJobs(OffsetDateTime.now(UTC).minus(hungJuridisk.toJavaDuration()))
            .let {
                if (it > 0) {
                    span.setAttribute("juridisk-henvisning-producer-job.jobs-reset", it.toString())
                    logger.info("Reset $it juridisk henvisning jobs for sending")
                }
            }

        var count = 0
        do {
            val (next, didProduce) = sendNextJuridiskHenvisning()
            if (next != null && didProduce) count++
        } while (next != null)

        if (count > 0)
            logger.info("Finished juridisk henvsining producer batch, sent $count henvisninger")

        span.setAttribute("juridisk-henvisning-producer-job.producer", count.toString())
    }

    private suspend fun sendNextJuridiskHenvisning(): Pair<JuridiskHenvisningJob?, Boolean> {
        val next = juridiskHenvisningJobRepo.getNext() ?: return null to false
        val sykmeldingId = next.sykmeldingId

        if (next.juridiskVurdering.isEmpty()) {
            juridiskHenvisningJobRepo.updateStatus(sykmeldingId, JuridiskVurderingStatus.DONE)
            return next to false
        }

        try {
            val juridiskVurderinger =
                next.juridiskVurdering.map {
                    it.toJuridiskVurdering(
                        sykmeldingId.toString(),
                        eventName = JURIDISK_HENVISNING_EVENT_NAME,
                        version = JURIDISK_HENVISNING_VERSION,
                        kilde = JURIDISK_HENVISNING_KILDE,
                        versjonAvKode = environment.runtime.version,
                        sporing = mapOf("sykmeldingId" to sykmeldingId.toString()),
                    )
                }

            juridiskHenvisningProducer.sendJuridiskVurderinger(sykmeldingId, juridiskVurderinger)
            juridiskHenvisningJobRepo.updateStatus(sykmeldingId, JuridiskVurderingStatus.DONE)

            return next to true
        } catch (e: Exception) {
            logger.error("Failed to produce juridisk henvisning for ID $sykmeldingId", e)
            juridiskHenvisningJobRepo.updateStatus(sykmeldingId, JuridiskVurderingStatus.FAILED)

            return next to false
        }
    }
}
