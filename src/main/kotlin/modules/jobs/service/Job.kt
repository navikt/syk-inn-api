package modules.jobs.service

import core.jobs.JobStatus
import java.time.OffsetDateTime

enum class JobName {
    SYKMELDING_CONSUMER
}

data class Job(
    val jobName: JobName,
    val desiredState: JobStatus,
    val updatedAt: OffsetDateTime,
    val updatedBy: String,
)

data class RunnerJobStatus(
    val runner: String,
    val job: JobName,
    val state: JobStatus,
    val updatedAt: OffsetDateTime,
)

enum class JobUpdateAction {
    START,
    STOP,
}

data class JobUpdatePayload(val state: JobUpdateAction)
