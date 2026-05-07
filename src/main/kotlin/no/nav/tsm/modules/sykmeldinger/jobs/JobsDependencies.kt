package no.nav.tsm.modules.sykmeldinger.jobs

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.core.Environment
import no.nav.tsm.core.dynamicDependencies
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskHenvisningJobRepo
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskHenvisningProducer
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskHenvisningProducerJob
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumer
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumerJob
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumerRepo
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumerResourcesService
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumerService
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.poison.SykmeldingPoisonPillRepo
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete.SykmeldingDeleteJob
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete.SykmeldingDeleteRepo
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce.SykmeldingProducerJob
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce.SykmeldingProducerRepo
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer

fun Application.configureJobsDependencies() {
    val environment: Environment by dependencies

    dependencies {
        provide(SykmeldingPoisonPillRepo::class)
        provide(SykmeldingConsumerRepo::class)
        provide(SykmeldingConsumer::class)
        provide(SykmeldingConsumerResourcesService::class)
        provide(SykmeldingConsumerService::class)
        provide(SykmeldingConsumerJob::class)

        provide(SykmeldingProducerRepo::class)
        provide(SykmeldingProducerJob::class)

        provide(SykmeldingDeleteRepo::class)
        provide(SykmeldingDeleteJob::class)

        provide(JuridiskHenvisningJobRepo::class)
        provide(JuridiskHenvisningProducer::class)
        provide(JuridiskHenvisningProducerJob::class)
    }

    dynamicDependencies {
        local {
            provide<SykmeldingInputProducer> {
                SykmeldingInputKafkaInputFactory.localProducer(
                    appname = environment.runtime.name,
                    namespace = "tsm",
                    properties = environment.kafka.config,
                )
            }
        }
        cloud {
            provide<SykmeldingInputProducer> { SykmeldingInputKafkaInputFactory.naisProducer() }
        }
    }
}
