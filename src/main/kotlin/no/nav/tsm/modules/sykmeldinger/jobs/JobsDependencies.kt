package no.nav.tsm.modules.sykmeldinger.jobs

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.kafka.producer.KafkaProducer
import no.nav.tsm.ktor.kafka.producer.KafkaRecordProducer
import no.nav.tsm.ktor.kafka.sykmeldinger.SykmeldingInputProducer
import no.nav.tsm.ktor.kafka.sykmeldinger.sykmeldingInputProducer
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskHenvisningJobRepo
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskHenvisningProducerJob
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskHenvisningRecord
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumerJob
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumerRepo
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumerResourcesService
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumerService
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.poison.SykmeldingPoisonPillRepo
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete.SykmeldingDeleteJob
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete.SykmeldingDeleteRepo
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce.SykmeldingProducerJob
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce.SykmeldingProducerRepo

fun Application.configureJobsDependencies() {
    val environment: Environment by dependencies

    install(KafkaProducer) { clientId = environment.runtime.name }

    dependencies {
        provide(SykmeldingPoisonPillRepo::class)
        provide(SykmeldingConsumerRepo::class)
        provide(SykmeldingConsumerResourcesService::class)
        provide(SykmeldingConsumerService::class)
        provide(SykmeldingConsumerJob::class)

        provide<SykmeldingInputProducer> {
            this@configureJobsDependencies.sykmeldingInputProducer()
        }
        provide(SykmeldingProducerRepo::class)
        provide(SykmeldingProducerJob::class)

        provide(SykmeldingDeleteRepo::class)
        provide(SykmeldingDeleteJob::class)

        provide<KafkaRecordProducer<JuridiskHenvisningRecord>> {
            KafkaRecordProducer.initProducer(
                application = this@configureJobsDependencies,
                topic = "teamsykmelding.paragraf-i-kode",
            )
        }
        provide(JuridiskHenvisningJobRepo::class)
        provide(JuridiskHenvisningProducerJob::class)
    }
}
