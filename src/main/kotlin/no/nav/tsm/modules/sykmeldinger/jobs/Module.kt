package no.nav.tsm.modules.sykmeldinger.jobs

import io.ktor.server.application.Application
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.configureSykmeldingConsumer

fun Application.configureKafkaModule() {
    configureJobsDependencies()
    configureSykmeldingConsumer()
}
