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
import no.nav.tsm.core.db.runFlywayMigrations
import no.nav.tsm.core.logger
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig

private val logger = logger()

fun Application.configureDatabase() {
    val env: Environment by dependencies

    runFlywayMigrations(env.postgres)

    R2dbcDatabase.connect(
        url = "r2dbc:${env.postgres.r2}",
        user = env.postgres.username,
        password = env.postgres.password,
        databaseConfig = createDbConfig(env.postgres),
    )
}

private fun createDbConfig(postgresConfig: PostgresConfig) = R2dbcDatabaseConfig {
    connectionFactoryOptions {
        option(SCHEMA, postgresConfig.schema)
        if (postgresConfig.sslKeyPk8.isNotEmpty()) {
            option(SSL_MODE, SSLMode.VERIFY_CA)
            val customizer =
                pkcs8KeyManagerCustomizer(
                    cert = Path.of(postgresConfig.sslCert),
                    pkcs8DerKey = Path.of(postgresConfig.sslKeyPk8),
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
        it.endpointIdentificationAlgorithm("")
            .keyManager(ByteArrayInputStream(certBytes), ByteArrayInputStream(pkcs8Pem))
    }
}
