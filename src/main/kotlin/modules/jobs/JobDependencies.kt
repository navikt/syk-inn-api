package no.nav.tsm.modules.jobs

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.core.Environment
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.modules.jobs.db.JobRepository
import no.nav.tsm.modules.jobs.service.JobSchedulerService
import no.nav.tsm.modules.kafka.consume.SykmeldingConsumerJob

fun Application.configureJobDependencies() {
    val consumerJobManager: SykmeldingConsumerJob by dependencies
    val environment: Environment by dependencies
    val list: List<Job> = listOf(consumerJobManager)

    dependencies {
        provide<List<Job>> { list }
        provide<String>("runner") { environment.runtime.name }
        provide(JobRepository::class)
        provide(JobSchedulerService::class)
    }
}
