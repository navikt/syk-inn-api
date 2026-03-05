package modules.kafka.consume

import core.jobs.JobManager
import kotlinx.coroutines.CoroutineScope
import modules.jobs.service.JobName

class SykmeldingConsumerJobManager(
    applicationScope: CoroutineScope,
    private val sykmeldingConsumerService: SykmeldingConsumerService,
) : JobManager(applicationScope) {
    override val jobName: JobName = JobName.SYKMELDING_CONSUMER

    override suspend fun runJob() {
        sykmeldingConsumerService.consume()
    }
}
