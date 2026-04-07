package no.nav.tsm.modules.jobs

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.core.Environment
import no.nav.tsm.core.jobs.Job
import no.nav.tsm.modules.jobs.db.JobRepository
import no.nav.tsm.modules.jobs.service.JobSchedulerService
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskHenvisningProducerJob
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumerJob
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete.SykmeldingDeleteJob
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce.SykmeldingProducerJob

fun Application.configureJobDependencies() {
    val consumerJobManager: SykmeldingConsumerJob by dependencies
    val sykmelidngProducerJob: SykmeldingProducerJob by dependencies
    val sykmeldingDeleteJob: SykmeldingDeleteJob by dependencies
    val juridiskHenvisningProducerJob: JuridiskHenvisningProducerJob by dependencies
    val environment: Environment by dependencies
    val list: List<Job> =
        listOf(
            consumerJobManager,
            sykmelidngProducerJob,
            sykmeldingDeleteJob,
            juridiskHenvisningProducerJob,
        )

    dependencies {
        provide<List<Job>> { list }
        provide<String>("runner") { environment.runtime.name }
        provide(JobRepository::class)
        provide(JobSchedulerService::class)
    }
}
