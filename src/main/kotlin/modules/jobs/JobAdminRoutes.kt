package modules.jobs

import core.jobs.JobStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.time.OffsetDateTime
import modules.jobs.service.JobName
import modules.jobs.service.JobService
import modules.jobs.service.JobUpdateAction
import modules.jobs.service.JobUpdatePayload

data class JobRunners(val runner: String, val state: JobStatus, val updatedAt: OffsetDateTime)

data class JobStatusResponse(
    val name: JobName,
    val runners: List<JobRunners>,
    val desiredState: JobStatus,
    val updatedAt: OffsetDateTime,
)

fun Application.configureJobAdminRoutes() {
    val jobService: JobService by dependencies

    routing {
        route("/internal/admin/jobs") {
            get {
                val jobs = jobService.getJobs()
                val statuses = jobService.getJobStatus().groupBy { it.job }
                val response =
                    jobs.map { job ->
                        val jobStatuses = statuses[job.jobName] ?: emptyList()
                        JobStatusResponse(
                            name = job.jobName,
                            desiredState = job.desiredState,
                            updatedAt = job.updatedAt,
                            runners =
                                jobStatuses.map { runner ->
                                    JobRunners(
                                        runner = runner.runner,
                                        state = runner.state,
                                        updatedAt = runner.updatedAt,
                                    )
                                },
                        )
                    }

                call.respond(HttpStatusCode.OK, response)
            }
            post("{name}/status") {
                val name =
                    call.parameters["name"]?.let { JobName.valueOf(it) }
                        ?: return@post call.respond(HttpStatusCode.BadRequest)
                val job = call.receive<JobUpdatePayload>()
                val desiredState =
                    when (job.desiredState) {
                        JobUpdateAction.START -> JobStatus.RUNNING
                        JobUpdateAction.STOP -> JobStatus.STOPPED
                    }

                jobService.updateJob(name, desiredState, job.updatedBy)

                call.respond(HttpStatusCode.Accepted, mapOf("ok" to true))
            }
        }
    }
}
