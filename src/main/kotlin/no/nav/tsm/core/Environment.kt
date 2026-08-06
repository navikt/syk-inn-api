package no.nav.tsm.core

import io.ktor.server.config.*
import kotlin.time.Duration
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.nais.getRuntimeCluster

class Runtime(val env: RuntimeCluster, val name: String, val version: String)

class SykmeldingConfig(val retention: Duration)

class PostgresR2DBCConfig(val url: String, val sslCert: String?, val sslKeyPk8: String?)

class PostgresConfig(
    val jdbc: String,
    val r2: PostgresR2DBCConfig,
    val username: String,
    val password: String,
    val schema: String,
)

class ExternalApi(val tsmPdlCache: String, val helsenettproxy: String, val btsys: String)

class ProducerJob(val delay: Duration, val hungTimeout: Duration)

class DeleterJob(val interval: Duration)

class KafkaSykmeldingConsumer(val longPoll: Duration)

class JobsConfig(
    val inputProducer: ProducerJob,
    val juridiskProducer: ProducerJob,
    val sykmeldingDeleter: DeleterJob,
)

class Environment(
    val runtime: Runtime,
    val jobs: JobsConfig,
    val sykmeldingConsumer: KafkaSykmeldingConsumer,
    val postgres: PostgresConfig,
    val sykmeldingConfig: SykmeldingConfig,
    val external: () -> ExternalApi,
)

fun initializeEnvironment(config: ApplicationConfig): Environment {
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
                env = getRuntimeCluster(),
                name = config.property("app.name").getString(),
                version = config.property("app.version").getString(),
            ),
        sykmeldingConsumer =
            KafkaSykmeldingConsumer(
                longPoll = config.property("kafka.sykmeldingConsumer.longPoll").getAs()
            ),
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
        external = {
            ExternalApi(
                tsmPdlCache = config.property("external.tsmPdlCache").getString(),
                helsenettproxy = config.property("external.syfoHelsenettproxy").getString(),
                btsys = config.property("external.btsys").getString(),
            )
        },
    )
}
