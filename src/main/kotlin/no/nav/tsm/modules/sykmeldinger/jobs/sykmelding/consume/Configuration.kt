package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.kafka.consumer.KafkaConsumerJob
import no.nav.tsm.ktor.kafka.consumer.createConsumer
import no.nav.tsm.ktor.kafka.consumer.onRecord
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord

fun Application.configureSykmeldingConsumer() {
    val env: Environment by dependencies
    val service: SykmeldingConsumerService by dependencies

    val consumerJob =
        createConsumer(
            groupId = "syk-inn-api-v2",
            pollDuration = env.sykmeldingConsumer.longPoll,
            topic =
                onRecord<SykmeldingRecord>(
                    name = "tsm.sykmeldinger",
                    onTombstone = { meta -> service.handleTombstone(meta.key) },
                    onRecord = { record -> service.handleRecord(record) },
                ),
            jacksonModules = listOf(SykmeldingModule()),
        )

    dependencies { provide<KafkaConsumerJob> { consumerJob } }
}
