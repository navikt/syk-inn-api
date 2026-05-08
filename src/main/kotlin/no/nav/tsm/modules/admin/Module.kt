package no.nav.tsm.modules.admin

import io.ktor.server.application.Application

fun Application.configureJobsModule() {
    configureJobDependencies()
    configureJobScheduling()
    configureJobAdminRoutes()
}
