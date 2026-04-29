package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce

import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import kotlin.time.toJavaDuration
import kotlinx.coroutines.*
import no.nav.tsm.core.Environment
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.core.logger
import no.nav.tsm.modules.jobs.service.JobName
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusStatus
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingRepo
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer

class SykmeldingProducerJob(
    private val sykmeldingProducer: SykmeldingInputProducer,
    private val sykmeldingProducerRepo: SykmeldingProducerRepo,
    private val sykmeldingRepo: SykmeldingRepo,
    environment: Environment,
    applicationScope: CoroutineScope,
) : Job(JobName.SYKMELDING_PRODUCER, applicationScope) {
    private val logger = logger()

    private val batchDelay = environment.jobs.inputProducer.delay
    private val hungSykmelding = environment.jobs.inputProducer.hungTimeout

    override suspend fun runJob() =
        withContext(Dispatchers.IO) {
            while (isActive) {
                delay(batchDelay)
                logger.debug("Running sykmeldinger producer job (${batchDelay}ms)")
                handleSykmeldingerBatch()
            }
        }

    @WithSpan
    private suspend fun handleSykmeldingerBatch() {
        sykmeldingProducerRepo
            .resetHangingJobs(OffsetDateTime.now(UTC).minus(hungSykmelding.toJavaDuration()))
            .let {
                if (it > 0) {
                    logger.info("Reset $it sykmeldinger for sending")
                }
            }

        var count = 0
        do {
            val next = sendNextSykmelding()
            if (next != null) count++
        } while (next != null)

        if (count > 0) logger.info("Finished sykmeldinger producer batch, sent $count sykmeldinger")
    }

    private suspend fun sendNextSykmelding(): SykmeldingStatusJob? {
        val next = sykmeldingProducerRepo.getNext() ?: return null

        try {
            val sykmelding =
                requireNotNull(sykmeldingRepo.byId(next.sykmeldingId)) {
                    "Sykmelding with id ${next.sykmeldingId} not found."
                }

            sykmeldingProducer.sendSykmelding(sykmelding.toInputRecord())

            sykmeldingProducerRepo.updateStatus(next.sykmeldingId, SykmeldingStatusStatus.SENT)
        } catch (ex: Exception) {
            logger.error("Failed to produce sykmelding for ID ${next.sykmeldingId}", ex)
            sykmeldingProducerRepo.updateStatus(next.sykmeldingId, SykmeldingStatusStatus.FAILED)
        }

        return next
    }
}
