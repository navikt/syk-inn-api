package no.nav.tsm.modules.kafka.jobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.core.jobs.JobManager
import no.nav.tsm.modules.jobs.service.JobName
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.clients.producer.KafkaProducer

class SykmeldingProducerJob(
    val sykmeldingProducer: KafkaProducer<String, SykmeldingRecord>,
    applicationScope: CoroutineScope,
) : JobManager(applicationScope) {

    override val jobName: JobName = JobName.SYKMELDING_PRODUCER

    override suspend fun runJob() = withContext(Dispatchers.Default) {
        while (isActive) {
            delay(10000)
            TODO("IMPLEMENT job that sends sykmelding to kafka")
        }
    }
}
