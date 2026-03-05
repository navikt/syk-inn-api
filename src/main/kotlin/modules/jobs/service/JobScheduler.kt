package modules.jobs.service

import core.jobs.JobManager
import core.jobs.JobStatus
import core.logger
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class JobScheduler(private val jobManagers: List<JobManager>, private val jobService: JobService) {
    private val updateInterval = 10.seconds
    private val log = logger()

    private val uuid = UUID.randomUUID().toString()

    suspend fun setup() {
        log.debug("Setting up JobScheduler")
        jobManagers.forEach {
            jobService.updateJobStatus(
                runner = uuid,
                jobName = it.jobName,
                jobStatus = it.status.value,
            )
        }
    }

    suspend fun start() = coroutineScope {
        jobManagers.forEach { manager ->
            launch {
                manager.status.collect { newStatus ->
                    log.debug("Job ${manager.jobName} status changed to $newStatus")
                    jobService.updateJobStatus(
                        runner = uuid,
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

    suspend fun updateJobs() {
        log.debug("Updating jobs statuses")
        val jobs = jobService.getJobs().associate { it.jobName to it.desiredState }
        jobManagers.forEach { manager ->
            val desiredState = jobs[manager.jobName]
            requireNotNull(desiredState) { "No desired state found for job ${manager.jobName}" }
            if (desiredState != manager.status.value) {
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
