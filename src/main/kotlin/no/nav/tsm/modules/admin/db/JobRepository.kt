package no.nav.tsm.modules.admin.db

import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.core.jobs.JobStatus
import no.nav.tsm.modules.admin.db.exposed.JobStatusTable
import no.nav.tsm.modules.admin.db.exposed.JobTable
import no.nav.tsm.modules.admin.service.Job
import no.nav.tsm.modules.admin.service.JobName
import no.nav.tsm.modules.admin.service.RunnerJobStatus
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.r2dbc.deleteReturning
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.upsert

class JobRepository {

    @WithSpan
    suspend fun getJobs(): List<Job> = dbQuery {
        JobTable.selectAll()
            .map {
                Job(
                    jobName = JobName.valueOf(it[JobTable.name]),
                    desiredState = JobStatus.valueOf(it[JobTable.desiredState]),
                    updatedAt = OffsetDateTime.ofInstant(it[JobTable.updatedAt], ZoneOffset.UTC),
                    updatedBy = it[JobTable.updatedBy],
                )
            }
            .toList()
    }

    @WithSpan(inheritContext = false)
    suspend fun updateJobStatus(runner: String, jobName: JobName, jobStatus: JobStatus) {
        dbQuery {
            JobStatusTable.upsert(
                where = { JobStatusTable.runner eq runner and (JobStatusTable.job eq jobName.name) }
            ) {
                it[JobStatusTable.runner] = runner
                it[JobStatusTable.job] = jobName.name
                it[JobStatusTable.state] = jobStatus.name
                it[JobStatusTable.updatedAt] = OffsetDateTime.now(ZoneOffset.UTC).toInstant()
            }
        }
    }

    @WithSpan
    suspend fun deleteRunner(runner: String) = dbQuery {
        JobStatusTable.deleteReturning { JobStatusTable.runner eq runner }
            .map {
                RunnerJobStatus(
                    runner = it[JobStatusTable.runner],
                    job = JobName.valueOf(it[JobStatusTable.job]),
                    state = JobStatus.valueOf(it[JobStatusTable.state]),
                    updatedAt =
                        OffsetDateTime.ofInstant(it[JobStatusTable.updatedAt], ZoneOffset.UTC),
                )
            }
            .toList()
    }

    suspend fun deleteOldJobsRunners() = dbQuery {
        JobStatusTable.deleteReturning {
                JobStatusTable.updatedAt less Instant.now().minusSeconds(3600)
            }
            .map {
                RunnerJobStatus(
                    runner = it[JobStatusTable.runner],
                    job = JobName.valueOf(it[JobStatusTable.job]),
                    state = JobStatus.valueOf(it[JobStatusTable.state]),
                    updatedAt =
                        OffsetDateTime.ofInstant(it[JobStatusTable.updatedAt], ZoneOffset.UTC),
                )
            }
            .toList()
    }

    @WithSpan
    suspend fun updateJob(jobName: JobName, desiredState: JobStatus, updatedBy: String) {
        dbQuery {
            JobTable.update({ JobTable.name eq jobName.name }) {
                it[JobTable.desiredState] = desiredState.name
                it[JobTable.updatedAt] = OffsetDateTime.now(ZoneOffset.UTC).toInstant()
                it[JobTable.updatedBy] = updatedBy
            }
        }
    }

    @WithSpan
    suspend fun getJobStatus(): List<RunnerJobStatus> {
        return dbQuery {
            JobStatusTable.selectAll()
                .map {
                    RunnerJobStatus(
                        runner = it[JobStatusTable.runner],
                        job = JobName.valueOf(it[JobStatusTable.job]),
                        state = JobStatus.valueOf(it[JobStatusTable.state]),
                        updatedAt =
                            OffsetDateTime.ofInstant(it[JobStatusTable.updatedAt], ZoneOffset.UTC),
                    )
                }
                .toList()
        }
    }

    suspend fun updateJobStatusTimestamp(runner: String) = dbQuery {
        JobStatusTable.update({ JobStatusTable.runner eq runner }) {
            it[JobStatusTable.updatedAt] = Instant.now()
        }
    }
}
