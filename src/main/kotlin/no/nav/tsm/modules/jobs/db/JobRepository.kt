package no.nav.tsm.modules.jobs.db

import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import no.nav.tsm.core.jobs.JobStatus
import no.nav.tsm.modules.jobs.db.exposed.JobStatusTable
import no.nav.tsm.modules.jobs.db.exposed.JobTable
import no.nav.tsm.modules.jobs.service.Job
import no.nav.tsm.modules.jobs.service.JobName
import no.nav.tsm.modules.jobs.service.RunnerJobStatus
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.upsert

class JobRepository {

    suspend fun getJobs(): List<Job> {
        return suspendTransaction {
            JobTable.selectAll()
                .map {
                    Job(
                        jobName = JobName.valueOf(it[JobTable.name]),
                        desiredState = JobStatus.valueOf(it[JobTable.desiredState]),
                        updatedAt =
                            OffsetDateTime.ofInstant(
                                it[JobTable.updatedAt].toJavaInstant(),
                                ZoneOffset.UTC,
                            ),
                        updatedBy = it[JobTable.updatedBy],
                    )
                }
                .toList()
        }
    }

    suspend fun updateJobStatus(runner: String, jobName: JobName, jobStatus: JobStatus) {
        suspendTransaction {
            JobStatusTable.upsert(
                where = { JobStatusTable.runner eq runner and (JobStatusTable.job eq jobName.name) }
            ) {
                it[JobStatusTable.runner] = runner
                it[JobStatusTable.job] = jobName.name
                it[JobStatusTable.state] = jobStatus.name
                it[JobStatusTable.updatedAt] =
                    OffsetDateTime.now(ZoneOffset.UTC).toInstant().toKotlinInstant()
            }
        }
    }

    suspend fun deleteRunner(runner: String) {
        suspendTransaction { JobStatusTable.deleteWhere { JobStatusTable.runner eq runner } }
    }

    suspend fun updateJob(jobName: JobName, desiredState: JobStatus, updatedBy: String) {
        suspendTransaction {
            JobTable.update({ JobTable.name eq jobName.name }) {
                it[JobTable.desiredState] = desiredState.name
                it[JobTable.updatedAt] =
                    OffsetDateTime.now(ZoneOffset.UTC).toInstant().toKotlinInstant()
                it[JobTable.updatedBy] = updatedBy
            }
        }
    }

    suspend fun getJobStatus(): List<RunnerJobStatus> {
        return suspendTransaction {
            JobStatusTable.selectAll()
                .map {
                    RunnerJobStatus(
                        runner = it[JobStatusTable.runner],
                        job = JobName.valueOf(it[JobStatusTable.job]),
                        state = JobStatus.valueOf(it[JobStatusTable.state]),
                        updatedAt =
                            OffsetDateTime.ofInstant(
                                it[JobStatusTable.updatedAt].toJavaInstant(),
                                ZoneOffset.UTC,
                            ),
                    )
                }
                .toList()
        }
    }
}
