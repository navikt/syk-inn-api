package no.nav.tsm.core.jobs

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.tsm.core.logger
import no.nav.tsm.modules.admin.service.JobName

enum class JobStatus {
    NOT_STARTED,
    RUNNING,
    STOPPED,
    FAILED,
}

abstract class Job(val jobName: JobName, private val applicationScope: CoroutineScope) {
    private val logger = logger()

    private var job: Job? = null
    private val _status = MutableStateFlow(JobStatus.NOT_STARTED)
    val status: StateFlow<JobStatus> = _status.asStateFlow()
    private val mutex: Mutex = Mutex()

    suspend fun start(): Boolean {
        logger.info("Starting Job($jobName)")

        if (job?.isActive == true) {
            logger.info("Job($jobName) is already running, not starting a new one")
            return false
        }

        mutex.withLock {
            if (job?.isActive == true) {
                logger.info(
                    "Job($jobName) was started by another request while waiting for lock, not starting a new one"
                )
                return false
            }

            job = applicationScope.launch {
                try {
                    _status.value = JobStatus.RUNNING
                    runJob()
                    _status.value = JobStatus.STOPPED
                } catch (_: CancellationException) {
                    logger.debug("Job(${jobName.name}) was cancelled gracefully")
                    _status.value = JobStatus.STOPPED
                } catch (cause: Exception) {
                    logger.error("Job(${jobName.name}) crashed unexpectedly", cause)
                    _status.value = JobStatus.FAILED
                } finally {
                    logger.info("Job(${jobName}) finished or failed, setting job reference null")
                    job = null
                }
            }
            return true
        }
    }

    suspend fun stop(): Boolean {
        if (job == null || job?.isCancelled == true) {
            logger.info("Job(${jobName.name}) was not running, no need to stop")

            _status.value = JobStatus.STOPPED
            return false
        }

        logger.info("Job(${jobName.name}) is running, stopping it ...")
        mutex.withLock {
            if (job == null || job?.isCancelled == true) {
                logger.info(
                    "Job(${jobName.name}) was already stopped by another request, no need to stop"
                )
                return false
            }

            job?.cancelAndJoin()
            job = null
            _status.value = JobStatus.STOPPED

            logger.info("Job(${jobName.name}) stopped successfully")
            return true
        }
    }

    abstract suspend fun runJob()
}
