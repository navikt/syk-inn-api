package no.nav.tsm.modules.sykmeldinger.jobs.juridisk

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.core.logger
import no.nav.tsm.modules.jobs.service.JobName
import no.nav.tsm.regulus.regula.juridisk.toJuridiskVurdering

class JuridiskHenvisningProducerJob(
    private val juridiskJobRepo: JuridiskJobRepo,
    applicationScope: CoroutineScope,
) : Job(JobName.JURIDISK_PRODUCER, applicationScope) {
    private val logger = logger()

    private val batchDelay = 5000.milliseconds

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
        val next = juridiskJobRepo.getNext()

        if (next == null) return

        val sykmeldingId = next.sykmeldingId.toString()
        val juridiskVurderinger =
            next.juridiskVurdering.map {
                it.toJuridiskVurdering(
                    sykmeldingId,
                    eventName = JuridiskHenvisningService.EVENT_NAME,
                    version = JuridiskHenvisningService.VERSION,
                    kilde = JuridiskHenvisningService.KILDE,
                    versjonAvKode = "TODO",
                    sporing = mapOf("sykmeldingId" to sykmeldingId),
                )
            }

        logger.info("Mapped to ${juridiskVurderinger.size} juridiske vurderinger!")

        TODO("IMPLEMENT job that sends juridiskHenvisning to kafka")
    }
}
