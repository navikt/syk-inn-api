package no.nav.tsm.modules.kafka.consume

import kotlinx.coroutines.CoroutineScope
import no.nav.tsm.core.jobs.JobManager

class SykmeldingConsumerJobManager(
    applicationScope: CoroutineScope,
    private val sykmeldingConsumerService: SykmeldingConsumerService,
) : JobManager(applicationScope) {
    override val jobName: String = "Sykmelding Consumer job"

    override suspend fun runJob() {
        sykmeldingConsumerService.consume()
    }
}
