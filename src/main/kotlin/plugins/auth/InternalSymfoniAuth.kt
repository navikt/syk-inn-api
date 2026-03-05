package plugins.auth

import com.auth0.jwk.JwkProviderBuilder
import core.Environment
import core.isLocal
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.routing.RoutingContext
import java.net.URI

val InternalSymfoniAuth = "internal-tsm-obo"

data class InternalSymfoniPrincipal(val name: String, val userId: String)

fun RoutingContext.internalSymfoniUser(): InternalSymfoniPrincipal =
    requireNotNull(this.call.principal<InternalSymfoniPrincipal>()) {
        "User not found in principal"
    }

/**
 * Configures authentication used for internal admin commands and requires a Team Symfoni team
 * member on-behalf-of token provided by EntraAD.
 */
fun Application.configureInternalSymfoniUserAuth() {
    val env: Environment by dependencies

    if (isLocal()) {
        configureLocalInternalSymfoniUserAuth()
        return
    }

    val entra = env.auth().entra
    val jwkProvider = JwkProviderBuilder(URI(entra.jwksUri).toURL()).build()

    authentication {
        jwt(InternalSymfoniAuth) {
            verifier(jwkProvider, entra.issuer) { withAudience(entra.audience) }
            validate { credential ->
                val name =
                    requireNotNull(credential.getClaim("name", String::class)) {
                        "Missing 'name' claim in token"
                    }
                val userId =
                    requireNotNull(credential.getClaim("preferred_username", String::class)) {
                        "Missing 'preferred_username' claim in token"
                    }

                InternalSymfoniPrincipal(name = name, userId = userId)
            }
        }
    }
}

private fun Application.configureLocalInternalSymfoniUserAuth() {
    log.error("Local development security, if you see this in production, something is very wrong!")

    val localPrincipal =
        InternalSymfoniPrincipal(name = "Local Symfoni User", userId = "local-symfoni-user")

    authentication {
        provider(InternalSymfoniAuth) { authenticate { ctx -> ctx.principal(localPrincipal) } }
    }
}
