package plugins.auth

import io.ktor.server.application.*

fun Application.configureAuthentication() {
    configureInternalSymfoniUserAuth()
}
