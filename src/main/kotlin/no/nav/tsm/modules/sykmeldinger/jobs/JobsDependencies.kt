package no.nav.tsm.modules.sykmeldinger.jobs

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.core.Environment
import no.nav.tsm.core.dynamicDependencies
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskHenvisningProducerJob
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskJobRepo
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumer
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumerJob
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumerRepo
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumerService
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete.SykmeldingDeleteJob
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce.SykmeldingProducerJob
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce.SykmeldingProducerRepo
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer

fun Application.configureJobsDependencies() {
    val environment: Environment by dependencies

    dependencies {
        provide<SykmeldingConsumerRepo>(SykmeldingConsumerRepo::class)
        provide<SykmeldingConsumer>(SykmeldingConsumer::class)
        provide<SykmeldingConsumerService>(SykmeldingConsumerService::class)
        provide<SykmeldingConsumerJob>(SykmeldingConsumerJob::class)

        provide<SykmeldingProducerRepo>(SykmeldingProducerRepo::class)
        provide<SykmeldingProducerJob>(SykmeldingProducerJob::class)

        provide<SykmeldingDeleteJob>(SykmeldingDeleteJob::class)
        provide<JuridiskJobRepo>(JuridiskJobRepo::class)
        provide<JuridiskHenvisningProducerJob>(JuridiskHenvisningProducerJob::class)
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
