package core.jobs

import core.logger
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
import modules.jobs.service.JobName

enum class JobStatus {
    NOT_STARTED,
    RUNNING,
    STOPPED,
    FAILED,
}

abstract class JobManager(private val applicationScope: CoroutineScope) {
    private val logger = logger()

    private var job: Job? = null
    private val _status = MutableStateFlow(JobStatus.NOT_STARTED)
    val status: StateFlow<JobStatus> = _status.asStateFlow()
    private val mutex: Mutex = Mutex()

    abstract val jobName: JobName

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
                        _status.value = JobStatus.RUNNING
                        runJob()
                        _status.value = JobStatus.STOPPED
                    } catch (ex: CancellationException) {
                        logger.info("KafkaConsumerJob was cancelled gracefully", ex)
                        _status.value = JobStatus.STOPPED
                    } catch (cause: Exception) {
                        logger.error("KafkaConsumerJob crashed unexpectedly", cause)
                        _status.value = JobStatus.FAILED
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

            _status.value = JobStatus.STOPPED
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
            _status.value = JobStatus.STOPPED

            logger.info("Job stopped successfully")
            return true
        }
    }

    abstract suspend fun runJob()
}
