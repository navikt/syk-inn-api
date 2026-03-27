package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import kotlinx.coroutines.CoroutineScope
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.modules.jobs.service.JobName

class SykmeldingConsumerJob(
    applicationScope: CoroutineScope,
    private val sykmeldingConsumerService: SykmeldingConsumerService,
) : Job(applicationScope) {
    override val jobName: JobName = JobName.SYKMELDING_CONSUMER

    override suspend fun runJob() {
        sykmeldingConsumerService.consume()
    }
}
