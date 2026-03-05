package modules.kafka.consume

import kotlinx.coroutines.CoroutineScope
import core.jobs.JobManager
import no.nav.tsm.modules.kafka.consume.SykmeldingConsumerService

class SykmeldingConsumerJobManager(
    applicationScope: CoroutineScope,
    private val sykmeldingConsumerService: SykmeldingConsumerService,
) : JobManager(applicationScope) {
    override val jobName: String = "Sykmelding Consumer job"

    override suspend fun runJob() {
        sykmeldingConsumerService.consume()
    }
}
