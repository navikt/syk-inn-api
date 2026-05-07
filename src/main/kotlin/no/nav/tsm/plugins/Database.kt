package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.netty.handler.ssl.SslContextBuilder
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider.SCHEMA
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider.SSL_CONTEXT_BUILDER_CUSTOMIZER
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider.SSL_MODE
import io.r2dbc.postgresql.client.SSLMode
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.function.Function
import no.nav.tsm.core.Environment
import no.nav.tsm.core.PostgresConfig
import no.nav.tsm.core.db.getFlyway
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig

fun Application.configureDatabase() {
    val env: Environment by dependencies

    getFlyway(env.postgres).migrate()

    R2dbcDatabase.connect(
        url = env.postgres.r2.url,
        user = env.postgres.username,
        password = env.postgres.password,
        databaseConfig = createR2DbcConfig(env.postgres),
    )
}

private fun createR2DbcConfig(postgresConfig: PostgresConfig) = R2dbcDatabaseConfig {
    connectionFactoryOptions {
        option(SCHEMA, postgresConfig.schema)
        if (postgresConfig.r2.sslKeyPk8 != null || postgresConfig.r2.sslCert != null) {
            requireNotNull(postgresConfig.r2.sslCert) {
                "SSL cert must be provided if SSL key is provided"
            }
            requireNotNull(postgresConfig.r2.sslKeyPk8) {
                "SSL key must be provided if SSL cert is provided"
            }

            option(SSL_MODE, SSLMode.VERIFY_CA)
            val customizer =
                pkcs8KeyManagerCustomizer(
                    cert = Path.of(postgresConfig.r2.sslCert),
                    pkcs8DerKey = Path.of(postgresConfig.r2.sslKeyPk8),
                )
            option(SSL_CONTEXT_BUILDER_CUSTOMIZER, customizer)
        }
    }
}

fun pkcs8KeyManagerCustomizer(
    cert: Path,
    pkcs8DerKey: Path,
): Function<SslContextBuilder, SslContextBuilder> {
    val certBytes = Files.readAllBytes(cert)
    val b64 =
        Base64.getMimeEncoder(64, byteArrayOf('\n'.code.toByte()))
            .encodeToString(Files.readAllBytes(pkcs8DerKey))
    val pkcs8Pem = "-----BEGIN PRIVATE KEY-----\n$b64\n-----END PRIVATE KEY-----\n".toByteArray()
    return Function {
        /**
         * Using R2DBC in nice requires us to disable endpoint identification/hostname verification
         * for the TLS connection
         */
        it.endpointIdentificationAlgorithm("")
            .keyManager(ByteArrayInputStream(certBytes), ByteArrayInputStream(pkcs8Pem))
    }
}
