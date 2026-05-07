package no.nav.tsm.core

import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.di.*
import java.util.*
import kotlin.time.Duration

enum class RuntimeEnvironments(val nais: String) {
    LOCAL("local"),
    DEV("dev-gcp"),
    PROD("prod-gcp"),
}

class Runtime(val env: RuntimeEnvironments, val name: String, val version: String)

class SykmeldingConfig(val retention: Duration)

class PostgresR2DBCConfig(val url: String, val sslCert: String?, val sslKeyPk8: String?)

class PostgresConfig(
    val jdbc: String,
    val r2: PostgresR2DBCConfig,
    val username: String,
    val password: String,
    val schema: String,
)

class Texas(val tokenEndpoint: String)

class ExternalApi(val tsmPdlCache: String, val helsenettproxy: String, val btsys: String)

class EntraAuth(val issuer: String, val jwksUri: String, val audience: String)

class Auth(val entra: EntraAuth)

class ProducerJob(val delay: Duration, val hungTimeout: Duration)

class DeleterJob(val interval: Duration)

class KafkaSykmeldingConsumer(val longPoll: Duration)

class KafkaConfig(val config: Properties, val sykmeldingConsumer: KafkaSykmeldingConsumer)

class JobsConfig(
    val inputProducer: ProducerJob,
    val juridiskProducer: ProducerJob,
    val sykmeldingDeleter: DeleterJob,
)

class Environment(
    val runtime: Runtime,
    val jobs: JobsConfig,
    val kafka: KafkaConfig,
    val postgres: PostgresConfig,
    val sykmeldingConfig: SykmeldingConfig,
    val texas: () -> Texas,
    val external: () -> ExternalApi,
    val auth: () -> Auth,
)

fun initializeEnvironment(config: ApplicationConfig): Environment {
    val kafkaProperties =
        KafkaConfig(
            config =
                Properties().apply {
                    config.config("kafka.config").toMap().forEach { this[it.key] = it.value }
                },
            sykmeldingConsumer =
                KafkaSykmeldingConsumer(
                    longPoll = config.property("kafka.sykmeldingConsumer.longPoll").getAs()
                ),
        )

    val jobsConfig =
        JobsConfig(
            inputProducer =
                ProducerJob(
                    delay = config.property("app.jobs.inputProducer.delay").getAs(),
                    hungTimeout = config.property("app.jobs.inputProducer.hungTimeout").getAs(),
                ),
            juridiskProducer =
                ProducerJob(
                    delay = config.property("app.jobs.juridiskProducer.delay").getAs(),
                    hungTimeout = config.property("app.jobs.juridiskProducer.hungTimeout").getAs(),
                ),
            sykmeldingDeleter =
                DeleterJob(
                    interval = config.property("app.jobs.sykmeldingDeleter.interval").getAs()
                ),
        )

    return Environment(
        runtime =
            Runtime(
                env = config.inferRuntimeEnvironment(),
                name = config.property("app.name").getString(),
                version = config.property("app.version").getString(),
            ),
        kafka = kafkaProperties,
        jobs = jobsConfig,
        postgres =
            PostgresConfig(
                jdbc = config.property("postgres.jdbc").getString(),
                r2 =
                    PostgresR2DBCConfig(
                        url = config.property("postgres.r2dbc.url").getString(),
                        sslCert = config.propertyOrNull("postgres.r2dbc.sslCert")?.getString(),
                        sslKeyPk8 = config.propertyOrNull("postgres.r2dbc.sslKeyPk8")?.getString(),
                    ),
                username = config.property("postgres.username").getString(),
                password = config.property("postgres.password").getString(),
                schema = config.property("postgres.schema").getString(),
            ),
        sykmeldingConfig =
            SykmeldingConfig(retention = config.property("app.sykmelding.retention").getAs()),
        texas = {
            Texas(tokenEndpoint = config.property("external.texas.tokenEndpoint").getString())
        },
        external = {
            ExternalApi(
                tsmPdlCache = config.property("external.tsmPdlCache").getString(),
                helsenettproxy = config.property("external.syfoHelsenettproxy").getString(),
                btsys = config.property("external.btsys").getString(),
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
    return when (val configEnv = this.property("app.runtime").getString()) {
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
