package no.nav.tsm.plugins.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.di.dependencies
import java.net.URI
import no.nav.tsm.core.Environment
import no.nav.tsm.core.isLocal

const val MACHINE_TOKEN_AUTH = "internal-entra-m2m"

fun Application.configureMachineTokenAuth() {
    val env: Environment by dependencies

    if (isLocal()) {
        configureLocalMachineTokenAuth()
        return
    }

    val entra = env.auth().entra
    val jwkProvider = JwkProviderBuilder(URI(entra.jwksUri).toURL()).build()

    authentication {
        jwt(MACHINE_TOKEN_AUTH) {
            verifier(jwkProvider, entra.issuer) { withAudience(entra.audience) }
            validate { credentials -> JWTPrincipal(credentials.payload) }
        }
    }
}

fun Application.configureLocalMachineTokenAuth() {
    authentication { provider(MACHINE_TOKEN_AUTH) { authenticate {} } }
}
