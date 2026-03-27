package no.nav.tsm.modules.sykmeldinger.jobs.juridisk

import kotlinx.coroutines.CoroutineScope
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.modules.jobs.service.JobName

class JuridiskHenvisningProducerJob(applicationScope: CoroutineScope) : Job(applicationScope) {
    override val jobName = JobName.JURIDISK_PRODUCER

    override suspend fun runJob() {}
}
