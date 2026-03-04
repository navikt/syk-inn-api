package no.nav.tsm.core.jobs

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.tsm.core.logger

enum class JobStatus {
    NOT_STARTED,
    RUNNING,
    STOPPED,
    FAILED,
}

abstract class JobManager(private val applicationScope: CoroutineScope) {
    private val logger = logger()

    private var job: Job? = null
    private var jobStatus: JobStatus = JobStatus.NOT_STARTED
    private val mutex: Mutex = Mutex()

    protected abstract val jobName: String

    fun status(): JobStatus {
        return this.jobStatus
    }

    suspend fun start(): Boolean {
        logger.info("Starting $jobName")

        if (job?.isActive == true) {
            logger.info("$jobName is already running, not starting a new one")
            return false
        }

        mutex.withLock {
            if (job?.isActive == true) {
                logger.info(
                    "$jobName was started by another request while waiting for lock, not starting a new one"
                )
                return false
            }

            job =
                applicationScope.launch {
                    try {
                        jobStatus = JobStatus.RUNNING
                        runJob()
                        jobStatus = JobStatus.STOPPED
                    } catch (ex: CancellationException) {
                        logger.info("KafkaConsumerJob was cancelled gracefully", ex)
                        jobStatus = JobStatus.STOPPED
                    } catch (cause: Exception) {
                        logger.error("KafkaConsumerJob crashed unexpectedly", cause)
                        jobStatus = JobStatus.FAILED
                    } finally {
                        logger.info("Job finished or failed, setting job reference to null")
                        job = null
                    }
                }
            return true
        }
    }

    suspend fun stop(): Boolean {
        if (job == null || job?.isCancelled == true) {
            logger.info("No job was running, nothing to stop")
            return false
        }

        logger.info("Job is running, stopping it ...")
        mutex.withLock {
            if (job == null || job?.isCancelled == true) {
                logger.info("Job was already stopped by another request, nothing to stop")
                return false
            }

            job?.cancelAndJoin()
            job = null
            jobStatus = JobStatus.STOPPED

            logger.info("Job stopped successfully")
            return true
        }
    }

    abstract suspend fun runJob()
}
