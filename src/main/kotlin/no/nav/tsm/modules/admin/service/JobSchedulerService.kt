package no.nav.tsm.modules.admin.service

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
import no.nav.tsm.modules.admin.db.JobRepository

class JobSchedulerService(
    private val jobs: List<Job>,
    private val jobRepository: JobRepository,
    @Named("runner") private val runner: String,
) {
    private val updateInterval = 10.seconds
    private val logger = logger()

    suspend fun setup() {
        logger.debug("Setting up JobScheduler")
        jobs.forEach {
            jobRepository.updateJobStatus(
                runner = runner,
                jobName = it.jobName,
                jobStatus = it.status.value,
            )
        }
    }

    suspend fun start() = coroutineScope {
        jobs.forEach { job ->
            launch {
                job.status.collect { newStatus ->
                    logger.debug("Job(${job.jobName}) status changed to $newStatus")
                    jobRepository.updateJobStatus(
                        runner = runner,
                        jobName = job.jobName,
                        jobStatus = newStatus,
                    )
                }
            }
        }

        jobRepository.deleteOldJobsRunners().onEach {
            logger.info("Old job runner: ${it.runner } deleted")
        }

        while (isActive) {
            updateJobs()
            delay(updateInterval)
            updateJobStatusTimestamps()
        }
    }

    private suspend fun updateJobStatusTimestamps() {
        jobRepository.updateJobStatusTimestamp(runner)
    }

    @WithSpan(inheritContext = false)
    suspend fun stop() {
        val span = Span.current()
        jobs.forEach { jobManager -> jobManager.stop() }
        val deleted =
            jobRepository.deleteRunner(runner).onEach {
                logger.info("Job runner: ${it.runner } deleted")
            }

        span.setAttribute("jobStatus.deleted.counter", deleted.size.toLong())
    }

    /** Don't @WithSpan this, as it causes every single span in all jobs to be nested under this. */
    private suspend fun updateJobs() {
        logger.debug("Updating jobs statuses")
        val jobStates = jobRepository.getJobs().associate { it.jobName to it.desiredState }

        val span = Span.current()
        span.setAttribute("job.count", jobStates.size.toLong())

        jobs.forEach { job ->
            val desiredState = jobStates[job.jobName]
            requireNotNull(desiredState) { "No desired state found for job ${job.jobName}" }
            if (desiredState != job.status.value) {
                logger.info("Updating job ${job.jobName} to desired state $desiredState")
                when (desiredState) {
                    JobStatus.RUNNING -> job.start()
                    JobStatus.STOPPED -> job.stop()
                    else ->
                        logger.warn("Unknown desired state $desiredState for job ${job.jobName}")
                }
            }
        }
    }
}
