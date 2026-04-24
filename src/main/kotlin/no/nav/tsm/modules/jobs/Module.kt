package no.nav.tsm.modules.jobs

import io.ktor.server.application.Application

fun Application.configureJobsModule() {
    configureJobDependencies()
    configureJobScheduling()
    configureJobAdminRoutes()
}
