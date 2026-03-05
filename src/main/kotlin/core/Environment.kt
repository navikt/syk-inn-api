package core

import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.di.dependencies
import java.util.Properties

enum class RuntimeEnvironments(val nais: String) {
    LOCAL("local"),
    DEV("dev-gcp"),
    PROD("prod-gcp"),
}

class Runtime(val env: RuntimeEnvironments, val name: String)

class PostgresConfig(val url: String, val username: String, val password: String)

class Texas(val tokenEndpoint: String)

class ExternalApi(val tsmPdlCache: String, val helsenettproxy: String, val btsys: String)

class EntraAuth(val issuer: String, val jwksUri: String, val audience: String)

class Auth(val entra: EntraAuth)

class Environment(
    val runtime: Runtime,
    val kafka: Properties,
    val postgres: PostgresConfig,
    val texas: () -> Texas,
    val external: () -> ExternalApi,
    val auth: () -> Auth,
)

fun initializeEnvironment(config: ApplicationConfig): Environment {
    val kafkaProperties =
        Properties().apply {
            config.config("kafka.config").toMap().forEach { this[it.key] = it.value }
        }

    return Environment(
        runtime =
            Runtime(
                env = config.inferRuntimeEnvironment(),
                name = config.property("app.name").getString(),
            ),
        kafka = kafkaProperties,
        postgres =
            PostgresConfig(
                url = config.property("postgres.url").getString(),
                username = config.property("postgres.username").getString(),
                password = config.property("postgres.password").getString(),
            ),
        texas = { Texas(tokenEndpoint = config.property("texas.token_endpoint").getString()) },
        external = {
            ExternalApi(
                tsmPdlCache = config.property("tsm-pdl-cache").getString(),
                helsenettproxy = config.property("syfohelsenettproxy").getString(),
                btsys = config.property("btsys").getString(),
            )
        },
        auth = {
            Auth(
                entra =
                    EntraAuth(
                        audience = config.property("auth.entra.audience").getString(),
                        jwksUri = config.property("auth.entra.openid.jwks").getString(),
                        issuer = config.property("auth.entra.openid.issuer").getString(),
                    )
            )
        },
    )
}

fun Application.isLocal(): Boolean {
    val env: Environment by dependencies

    return env.runtime.env == RuntimeEnvironments.LOCAL
}

private fun ApplicationConfig.inferRuntimeEnvironment(): RuntimeEnvironments {
    val configEnv = this.property("app.runtime").getString()

    return when (configEnv) {
        "local" -> RuntimeEnvironments.LOCAL
        "prod-gcp" -> RuntimeEnvironments.PROD
        "dev-gcp" -> RuntimeEnvironments.DEV
        else -> {
            throw IllegalStateException(
                "Unexpected 'app.runtime' configuration: ${configEnv}. Should be one of 'local', 'dev-gcp' or 'prod-gcp'"
            )
        }
    }
}
