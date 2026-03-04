package no.nav.tsm.core

import io.ktor.server.config.ApplicationConfig
import java.util.Properties

class PostgresConfig(
    val url: String,
    val username: String,
    val password: String,
)

class Environment(
    val kafka: Properties,
    val postgres: PostgresConfig,
)

fun initializeEnvironment(config: ApplicationConfig): Environment {
    val kafkaProperties =
        Properties().apply {
            config.config("kafka.config").toMap().forEach { this[it.key] = it.value }
        }

    return Environment(
        kafka = kafkaProperties,
        postgres = PostgresConfig(
            url = config.property("postgres.url").getString(),
            username = config.property("postgres.username").getString(),
            password = config.property("postgres.password").getString(),
        )
    )
}
