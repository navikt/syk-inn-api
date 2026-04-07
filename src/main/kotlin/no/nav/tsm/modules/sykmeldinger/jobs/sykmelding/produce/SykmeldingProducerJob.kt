package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce

import kotlinx.coroutines.*
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.modules.jobs.service.JobName
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer

class SykmeldingProducerJob(
    val sykmeldingProducer: SykmeldingInputProducer,
    val sykmeldingProducerRepo: SykmeldingProducerRepo,
    applicationScope: CoroutineScope,
) : Job(applicationScope) {

    override val jobName: JobName = JobName.SYKMELDING_PRODUCER

    override suspend fun runJob() =
        withContext(Dispatchers.Default) {
            while (isActive) {
                delay(10000)
                TODO("IMPLEMENT job that sends sykmelding to kafka")
            }
        }
}
