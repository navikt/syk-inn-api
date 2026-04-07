package no.nav.tsm.modules.sykmeldinger.jobs

import io.ktor.server.application.Application

fun Application.configureKafkaModule() {
    configureJobsDependencies()
}
