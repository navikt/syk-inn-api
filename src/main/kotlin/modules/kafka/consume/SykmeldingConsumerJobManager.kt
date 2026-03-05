package modules.kafka.consume

import core.jobs.JobManager
import kotlinx.coroutines.CoroutineScope

class SykmeldingConsumerJobManager(
    applicationScope: CoroutineScope,
    private val sykmeldingConsumerService: SykmeldingConsumerService,
) : JobManager(applicationScope) {
    override val jobName: String = "Sykmelding Consumer job"

    override suspend fun runJob() {
        sykmeldingConsumerService.consume()
    }
}
