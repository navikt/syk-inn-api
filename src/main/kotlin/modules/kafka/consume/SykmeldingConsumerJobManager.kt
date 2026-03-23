package no.nav.tsm.modules.kafka.consume

import kotlinx.coroutines.CoroutineScope
import no.nav.tsm.core.jobs.JobManager
import no.nav.tsm.modules.jobs.service.JobName

class SykmeldingConsumerJobManager(
    applicationScope: CoroutineScope,
    private val sykmeldingConsumerService: SykmeldingConsumerService,
) : JobManager(applicationScope) {
    override val jobName: JobName = JobName.SYKMELDING_CONSUMER

    override suspend fun runJob() {
        sykmeldingConsumerService.consume()
    }
}
