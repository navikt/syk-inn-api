package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.core.Environment
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.core.logger
import no.nav.tsm.modules.jobs.service.JobName

class SykmeldingDeleteJob(environment: Environment, applicationScope: CoroutineScope) :
    Job(JobName.SYKMELDING_DELETE, applicationScope) {
    private val logger = logger()

    private val jobInterval = environment.jobs.sykmeldingDeleter.interval

    override suspend fun runJob() {
        withContext(Dispatchers.IO) {
            while (isActive) {
                delay(jobInterval)

                logger.debug("Running delete old sykmeldinger job (${jobInterval.inWholeMinutes}m)")
                executeSykmeldingDeleteJob()
            }
        }
    }

    @WithSpan
    private fun executeSykmeldingDeleteJob() {
        println("tihi")
    }
}
