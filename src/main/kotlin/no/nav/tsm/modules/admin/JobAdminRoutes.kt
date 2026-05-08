package no.nav.tsm.modules.admin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.time.OffsetDateTime
import java.util.UUID
import no.nav.tsm.core.jobs.JobStatus
import no.nav.tsm.core.logger
import no.nav.tsm.modules.admin.db.JobRepository
import no.nav.tsm.modules.admin.service.JobName
import no.nav.tsm.modules.admin.service.JobUpdateAction
import no.nav.tsm.modules.admin.service.JobUpdatePayload
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.poison.SykmeldingPoisonPillRepo
import no.nav.tsm.plugins.auth.INTERNAL_SYMFONI_AUTH
import no.nav.tsm.plugins.auth.internalSymfoniUser

data class JobRunners(val runner: String, val state: JobStatus, val updatedAt: OffsetDateTime)

data class JobStatusResponse(
    val name: JobName,
    val runners: List<JobRunners>,
    val desiredState: JobStatus,
    val updatedAt: OffsetDateTime,
)

fun Application.configureJobAdminRoutes() {
    val logger = logger()
    val jobRepository: JobRepository by dependencies
    val poisonPillRepository: SykmeldingPoisonPillRepo by dependencies

    routing {
        authenticate(INTERNAL_SYMFONI_AUTH) {
            route("/internal/admin/poison-pills/{uuid}") {
                put {
                    val uuid =
                        call.parameters["uuid"]?.let { UUID.fromString(it) }
                            ?: return@put call.respond(HttpStatusCode.BadRequest)

                    val principal = internalSymfoniUser()
                    val poisoned =
                        poisonPillRepository.poison(uuid, "Marked as poison by ${principal.userId}")
                    call.respond(HttpStatusCode.OK, poisoned)
                }
            }
            route("/internal/admin/jobs") {
                get {
                    val jobs = jobRepository.getJobs()
                    val statuses = jobRepository.getJobStatus().groupBy { it.job }
                    val response = jobs.map { job ->
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
                        when (job.state) {
                            JobUpdateAction.START -> JobStatus.RUNNING
                            JobUpdateAction.STOP -> JobStatus.STOPPED
                        }

                    val principal = internalSymfoniUser()

                    logger.info(
                        "User ${principal.name} has requested to change the status of job $name to $desiredState"
                    )

                    jobRepository.updateJob(name, desiredState, principal.userId)

                    call.respond(HttpStatusCode.Accepted, mapOf("ok" to true))
                }
            }
        }
    }
}
