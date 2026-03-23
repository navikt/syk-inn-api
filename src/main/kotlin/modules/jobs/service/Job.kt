package no.nav.tsm.modules.jobs.service

import java.time.OffsetDateTime
import no.nav.tsm.core.jobs.JobStatus

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
