package no.nav.tsm.modules.sykmeldinger.jobs.juridisk

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
import no.nav.tsm.modules.jobs.service.JobName
import no.nav.tsm.regulus.regula.juridisk.toJuridiskVurdering

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

    private val batchDelay = environment.kafka.juridiskProducer.delay
    private val hungJuridisk = environment.kafka.juridiskProducer.hungTimeout

    override suspend fun runJob() =
        withContext(Dispatchers.IO) {
            while (isActive) {
                delay(batchDelay)

                logger.debug("Running juridisk henvisning producer job (${batchDelay}ms)")
                handleJuridiskHenvisningBatch()
            }
        }

    @WithSpan
    private suspend fun handleJuridiskHenvisningBatch() {
        juridiskHenvisningJobRepo
            .resetHangingJobs(OffsetDateTime.now(UTC).minus(hungJuridisk.toJavaDuration()))
            .let {
                if (it > 0) {
                    logger.info("Reset $it juridisk henvisning jobs for sending")
                }
            }

        var count = 0
        do {
            val next = sendNextJuridiskHenvisning()
            if (next != null) count++
        } while (next != null)

        if (count > 0)
            logger.info("Finished juridisk henvsining producer batch, sent $count henvisninger")
    }

    private suspend fun sendNextJuridiskHenvisning(): JuridiskHenvisningJob? {
        val next = juridiskHenvisningJobRepo.getNext() ?: return null
        val sykmeldingId = next.sykmeldingId

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
            juridiskHenvisningJobRepo.updateStatus(sykmeldingId, JuridiskVurderingStatus.SENT)
        } catch (e: Exception) {
            logger.error("Failed to produce juridisk henvisning for ID $sykmeldingId", e)
            juridiskHenvisningJobRepo.updateStatus(sykmeldingId, JuridiskVurderingStatus.FAILED)
        }

        return next
    }
}
