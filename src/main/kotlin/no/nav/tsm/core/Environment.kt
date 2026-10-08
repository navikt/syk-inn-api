package no.nav.tsm.core

import io.ktor.server.config.*
import kotlin.time.Duration
import no.nav.tsm.ktor.logger
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.nais.getRuntimeCluster

private val logger = logger()

class Runtime(val env: RuntimeCluster, val name: String, val sourceVersionPermalink: String)

class SykmeldingConfig(val retention: Duration)

class PostgresConfig(
    val jdbc: String,
    val username: String,
    val password: String,
    val schema: String,
)

class ExternalApi(val tsmBehandler: String)

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
    val env = getRuntimeCluster()

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
                env = env,
                name = config.property("app.name").getString(),
                sourceVersionPermalink = createSourcePermalink(env),
            ),
        sykmeldingConsumer =
            KafkaSykmeldingConsumer(
                longPoll = config.property("kafka.sykmeldingConsumer.longPoll").getAs()
            ),
        jobs = jobsConfig,
        postgres =
            PostgresConfig(
                jdbc = config.property("postgres.jdbc").getString(),
                username = config.property("postgres.username").getString(),
                password = config.property("postgres.password").getString(),
                schema = config.property("postgres.schema").getString(),
            ),
        sykmeldingConfig =
            SykmeldingConfig(retention = config.property("app.sykmelding.retention").getAs()),
        external = {
            ExternalApi(tsmBehandler = config.property("external.tsmBehandler").getString())
        },
    )
}

private fun createSourcePermalink(env: RuntimeCluster): String {
    if (env != RuntimeCluster.LOCAL && BuildInfo.GIT_SHA == "local") {
        logger.error(
            "Running in ${env.name} environment but BuildInfo.GIT_SHA is 'local'. This indicates that the application was built without a proper git SHA. Please ensure that the build process includes the git SHA for non-local environments."
        )
    }

    return "https://github.com/syk-inn-api/tree/${BuildInfo.GIT_SHA}"
}
