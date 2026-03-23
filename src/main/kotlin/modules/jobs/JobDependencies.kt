package no.nav.tsm.modules.jobs

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.core.Environment
import no.nav.tsm.core.jobs.JobManager
import no.nav.tsm.modules.jobs.db.JobRepository
import no.nav.tsm.modules.jobs.service.JobSchedulerService
import no.nav.tsm.modules.kafka.consume.SykmeldingConsumerJobManager

fun Application.configureJobDependencies() {
    val consumerJobManager: SykmeldingConsumerJobManager by dependencies
    val environment: Environment by dependencies
    val list: List<JobManager> = listOf(consumerJobManager)

    dependencies {
        provide<List<JobManager>> { list }
        provide<String>("runner") { environment.runtime.name }
        provide(JobRepository::class)
        provide(JobSchedulerService::class)
    }
}
