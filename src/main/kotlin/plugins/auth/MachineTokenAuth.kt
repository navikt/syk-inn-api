package no.nav.tsm.plugins.auth

import com.auth0.jwk.JwkProviderBuilder
import core.Environment
import core.isLocal
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.di.dependencies
import java.net.URI

val MachineTokenAuth = "internal-entra-m2m"

fun Application.configureMachineTokenAuth() {
    val env: Environment by dependencies

    if (isLocal()) {
        configureLocalMachineTokenAuth()
        return
    }

    val entra = env.auth().entra
    val jwkProvider = JwkProviderBuilder(URI(entra.jwksUri).toURL()).build()

    authentication {
        jwt(MachineTokenAuth) {
            verifier(jwkProvider, entra.issuer) { withAudience(entra.audience) }
        }
    }
}

fun Application.configureLocalMachineTokenAuth() {
    authentication { provider(MachineTokenAuth) { authenticate {} } }
}
