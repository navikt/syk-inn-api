package no.nav.tsm.core

import com.typesafe.config.ConfigFactory
import io.kotest.matchers.nulls.shouldNotBeNull
import io.ktor.server.config.HoconApplicationConfig
import kotlin.test.Test

class EnvironmentTest {
    @Test
    fun `production environment should be properly configured, even lazy values`() {
        val applicationConfig =
            HoconApplicationConfig(
                ConfigFactory.parseMap(
                        mapOf(
                            // Nais injected values
                            "NAIS_POD_NAME" to "syk-inn-api-prod-123",
                            "NAIS_CLUSTER_NAME" to "prod-gcp",
                            "AZURE_APP_CLIENT_ID" to "azure-client-id",
                            "AZURE_OPENID_CONFIG_ISSUER" to
                                "https://login.microsoftonline.com/test/v2.0",
                            "AZURE_OPENID_CONFIG_JWKS_URI" to
                                "https://login.microsoftonline.com/test/discovery/v2.0/keys",
                            "DB_JDBC_URL" to "jdbc:postgresql://db-host:5432/sykinn",
                            "DB_HOST" to "db-host",
                            "DB_PORT" to "5432",
                            "DB_DATABASE" to "sykinn",
                            "DB_SSLROOTCERT" to "/var/run/secrets/db/ca.pem",
                            "DB_SSLCERT" to "/var/run/secrets/db/client-cert.pem",
                            "DB_SSLKEY_PK8" to "/var/run/secrets/db/client-key.pk8",
                            "DB_USERNAME" to "db-user",
                            "DB_PASSWORD" to "db-password",
                            "KAFKA_BROKERS" to "kafka-1:9092,kafka-2:9092",
                            "KAFKA_TRUSTSTORE_PATH" to "/var/run/secrets/kafka/truststore.jks",
                            "KAFKA_CREDSTORE_PASSWORD" to "credstore-password",
                            "KAFKA_KEYSTORE_PATH" to "/var/run/secrets/kafka/keystore.p12",
                            "NAIS_TOKEN_ENDPOINT" to "https://texas/token",
                        )
                    )
                    .withFallback(ConfigFactory.parseResources("application.conf"))
                    .resolve()
            )

        val environment = initializeEnvironment(applicationConfig)

        // Poke lazy envs as well to ensure they are properly configured
        environment.texas().shouldNotBeNull()
        environment.external().shouldNotBeNull()
        environment.auth().shouldNotBeNull()
    }
}
