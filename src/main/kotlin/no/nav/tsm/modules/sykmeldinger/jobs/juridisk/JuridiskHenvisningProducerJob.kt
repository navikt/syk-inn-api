package no.nav.tsm.modules.sykmeldinger.jobs.juridisk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.modules.jobs.service.JobName

class JuridiskHenvisningProducerJob(applicationScope: CoroutineScope) :
    Job(JobName.JURIDISK_PRODUCER, applicationScope) {
    override suspend fun runJob() =
        withContext(Dispatchers.IO) {
            while (isActive) {
                delay(10000)
                TODO("IMPLEMENT job that sends juridiskHenvisning to kafka")
            }
        }
}
