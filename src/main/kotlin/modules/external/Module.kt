package modules.external

import io.ktor.server.application.Application

fun Application.configureExternalModule() {
    configureExternalDependencies()
}
