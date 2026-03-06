package modules.jobs

import core.Environment
import core.jobs.JobManager
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import modules.jobs.db.JobRepository
import modules.jobs.service.JobSchedulerService
import modules.kafka.consume.SykmeldingConsumerJobManager

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
