package no.nav.tsm.core

import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.di.dependencies
import java.util.Properties

enum class RuntimeEnvironments {
    LOCAL,
    DEV,
    PROD,
}

class PostgresConfig(val url: String, val username: String, val password: String)

class Texas(val tokenEndpoint: String)

class ExternalApi(val tsmPdlCache: String)

class Environment(
    val runtimeEnv: RuntimeEnvironments,
    val kafka: Properties,
    val postgres: PostgresConfig,
    val texas: () -> Texas,
    val external: () -> ExternalApi,
)

fun initializeEnvironment(config: ApplicationConfig): Environment {
    val kafkaProperties =
        Properties().apply {
            config.config("kafka.config").toMap().forEach { this[it.key] = it.value }
        }

    return Environment(
        runtimeEnv = config.inferRuntimeEnvironment(),
        kafka = kafkaProperties,
        postgres =
            PostgresConfig(
                url = config.property("postgres.url").getString(),
                username = config.property("postgres.username").getString(),
                password = config.property("postgres.password").getString(),
            ),
        texas = { Texas(tokenEndpoint = config.property("texas.token_endpoint").getString()) },
        external = { ExternalApi(tsmPdlCache = config.property("tsm.pdl_cache").getString()) },
    )
}

fun Application.isLocal(): Boolean {
    val env: Environment by dependencies

    return env.runtimeEnv == RuntimeEnvironments.LOCAL
}

private fun ApplicationConfig.inferRuntimeEnvironment(): RuntimeEnvironments {
    val configEnv = this.property("app.runtime").getString()

    return when (configEnv) {
        "local" -> RuntimeEnvironments.LOCAL
        "cloud" -> this.inferRuntimeFromNaisCluster()
        else ->
            throw IllegalStateException(
                "Unexpected 'app.runtime' configuration: ${configEnv}. Should be one of 'local' or 'cloud'."
            )
    }
}

private fun ApplicationConfig.inferRuntimeFromNaisCluster(): RuntimeEnvironments {
    val naisEnvironment = this.property("app.nais_cluster").getString()

    return when (naisEnvironment) {
        "dev" -> RuntimeEnvironments.DEV
        "prod" -> RuntimeEnvironments.PROD
        else ->
            throw IllegalStateException(
                "Unexpected 'app.nais_cluster' configuration: ${naisEnvironment}. Should be one of 'dev' or 'prod'."
            )
    }
}
