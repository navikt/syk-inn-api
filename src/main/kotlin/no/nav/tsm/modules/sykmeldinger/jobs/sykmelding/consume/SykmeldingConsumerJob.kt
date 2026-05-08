package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import kotlinx.coroutines.CoroutineScope
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.modules.admin.service.JobName

class SykmeldingConsumerJob(
    applicationScope: CoroutineScope,
    private val sykmeldingConsumerService: SykmeldingConsumerService,
) : Job(JobName.SYKMELDING_CONSUMER, applicationScope) {
    override suspend fun runJob() {
        sykmeldingConsumerService.consume()
    }
}
