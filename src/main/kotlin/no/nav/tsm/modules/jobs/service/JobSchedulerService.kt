package no.nav.tsm.modules.jobs.service

import io.ktor.server.plugins.di.annotations.Named
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.core.jobs.JobStatus
import no.nav.tsm.core.logger
import no.nav.tsm.modules.jobs.db.JobRepository

class JobSchedulerService(
    private val jobs: List<Job>,
    private val jobRepository: JobRepository,
    @Named("runner") private val runner: String,
) {
    private val updateInterval = 10.seconds
    private val log = logger()

    suspend fun setup() {
        log.debug("Setting up JobScheduler")
        jobs.forEach {
            jobRepository.updateJobStatus(
                runner = runner,
                jobName = it.jobName,
                jobStatus = it.status.value,
            )
        }
    }

    suspend fun start() = coroutineScope {
        jobs.forEach { manager ->
            launch {
                manager.status.collect { newStatus ->
                    log.debug("Job ${manager.jobName} status changed to $newStatus")
                    jobRepository.updateJobStatus(
                        runner = runner,
                        jobName = manager.jobName,
                        jobStatus = newStatus,
                    )
                }
            }
        }
        while (isActive) {
            updateJobs()
            delay(updateInterval)
        }
    }

    suspend fun stop() {
        jobs.forEach { jobManager -> jobManager.stop() }
        jobRepository.deleteRunner(runner)
    }

    @WithSpan
    private suspend fun updateJobs() {
        log.debug("Updating jobs statuses")
        val jobStates = jobRepository.getJobs().associate { it.jobName to it.desiredState }

        val span = Span.current()
        span.setAttribute("job.count", jobStates.size.toLong())

        jobs.forEach { job ->
            val desiredState = jobStates[job.jobName]
            requireNotNull(desiredState) { "No desired state found for job ${job.jobName}" }
            if (desiredState != job.status.value) {
                log.info("Updating job ${job.jobName} to desired state $desiredState")
                when (desiredState) {
                    JobStatus.RUNNING -> job.start()
                    JobStatus.STOPPED -> job.stop()
                    else -> log.warn("Unknown desired state $desiredState for job ${job.jobName}")
                }
            }
        }
    }
}
