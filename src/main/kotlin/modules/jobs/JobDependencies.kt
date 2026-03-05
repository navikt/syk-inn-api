package modules.jobs

import core.Environment
import core.jobs.JobManager
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import modules.jobs.service.JobScheduler
import modules.jobs.service.JobService
import modules.kafka.consume.SykmeldingConsumerJobManager

fun Application.configureJobDependencies() {
    val consumerJobManager: SykmeldingConsumerJobManager by dependencies
    val environment: Environment by dependencies
    val list: List<JobManager> = listOf(consumerJobManager)

    dependencies {
        provide<List<JobManager>> { list }
        provide<String>("runner") { environment.runtime.name }
        provide(JobService::class)
        provide(JobScheduler::class)
    }
}
