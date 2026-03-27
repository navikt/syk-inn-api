package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete

import kotlinx.coroutines.CoroutineScope
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.modules.jobs.service.JobName

class SykmeldingDeleteJob(applicationScope: CoroutineScope) : Job(applicationScope) {
    override val jobName = JobName.SYKMELDING_DELETE

    override suspend fun runJob() {
        TODO("Not yet implemented")
    }
}
