package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete

import kotlinx.coroutines.CoroutineScope
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.modules.jobs.service.JobName

class SykmeldingDeleteJob(applicationScope: CoroutineScope) :
    Job(JobName.SYKMELDING_DELETE, applicationScope) {
    override suspend fun runJob() {
        // TODO
        TODO("Not yet implemented")
    }
}
