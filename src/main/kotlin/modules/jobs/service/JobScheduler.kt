package modules.jobs.service

import core.jobs.JobManager
import core.jobs.JobStatus
import core.logger
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class JobScheduler(private val jobManagers: List<JobManager>, private val jobService: JobService) {
    private val updateInterval = 10.seconds
    private val log = logger()

    private val uuid = UUID.randomUUID().toString()

    suspend fun setup() {
        log.info("Setting up JobScheduler")
        updateJobStatuses()
    }

    private suspend fun updateJobStatuses() {
        log.info("Updating job statuses")
        jobManagers.forEach {
            jobService.updateJobStatus(runner = uuid, jobName = it.jobName, jobStatus = it.status())
        }
    }

    suspend fun start() = coroutineScope {
        while (isActive) {
            updateJobs()
            delay(updateInterval)
        }
    }

    suspend fun updateJobs() {
        log.info("Updating jobs statuses")
        updateJobStatuses()
        val jobs = jobService.getJobs().associate { it.jobName to it.desiredState }
        jobManagers.forEach { manager ->
            val desiredState = jobs[manager.jobName]
            requireNotNull(desiredState) { "No desired state found for job ${manager.jobName}" }
            if (desiredState != manager.status()) {
                log.info("Updating job ${manager.jobName} to desired state $desiredState")
                when (desiredState) {
                    JobStatus.RUNNING -> manager.start()
                    JobStatus.STOPPED -> manager.stop()
                    else ->
                        log.warn("Unknown desired state $desiredState for job ${manager.jobName}")
                }
            }
        }
    }

    suspend fun stop() {
        jobManagers.forEach { jobManager -> jobManager.stop() }
        jobService.deleteRunner(uuid)
    }
}
