package no.nav.tsm.modules.sykmeldinger.jobs

import io.ktor.server.application.Application
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.configureSykmeldingerKafkaConsumerDependencies

fun Application.configureKafkaModule() {
    configureSykmeldingerKafkaConsumerDependencies()
}
