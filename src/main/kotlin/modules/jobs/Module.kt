package modules.jobs

import io.ktor.server.application.Application

fun Application.configureJobsModule() {
    configureJobDependencies()
    configureJobSceduling()
    configureJobAdminRoutes()
}
