package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.Base64
import no.nav.tsm.core.Environment
import no.nav.tsm.core.db.runFlywayMigrations
import no.nav.tsm.core.isLocal
import no.nav.tsm.core.logger
import no.nav.tsm.core.teamLogger
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

private val logger = logger()
private val teamLog = teamLogger()

fun Application.configureDatabase() {
    val env: Environment by dependencies

    logger.info("Database URL: ${env.postgres.url}")

    if (!isLocal()) {
        fixDaFile()
    }

    runFlywayMigrations(env.postgres)

    val r2dbcUrl =
        "r2dbc:${env.postgres.url}"
            .let { url ->
                val separator = if ('?' in url) "&" else "?"
                "$url${separator}schema=${env.postgres.schema}"
            }
            .replace("sslkey", "sslKey")
            .replace("sslcert", "sslCert")
            .replace("sslrootcert", "sslRootCert")
            .replace("sslmode", "sslMode")

    logger.info("R2DBC URL: $r2dbcUrl")

    R2dbcDatabase.connect(
        url = r2dbcUrl,
        user = env.postgres.username,
        password = env.postgres.password,
    )
}

private fun fixDaFile() {
    val pk8Path = System.getenv("DB_SSLKEY_PK8")
    val content: ByteArray = File(pk8Path).readBytes()

    val encoder = Base64.getMimeEncoder(64, "\n".toByteArray())

    val string = StringBuilder()
    string.append("-----BEGIN PRIVATE KEY-----\n")
    string.append(encoder.encodeToString(content))
    string.append("\n-----END PRIVATE KEY-----")

    val outputPath = File("/tmp/fixed.pem").toPath()
    Files.writeString(outputPath, string.toString())
    Files.setPosixFilePermissions(outputPath, PosixFilePermissions.fromString("rw-------"))

    teamLog.info("Fixed the pk8 file maybe (pem)")
    teamLog.info(string.toString())

    teamLog.info("Other files")
    readAndTeamLog(System.getenv("DB_SSLCERT"))
    readAndTeamLog(System.getenv("DB_SSLROOTCERT"))
    readAndTeamLog(System.getenv("DB_SSLKEY"))
    readAndTeamLog(System.getenv("DB_SSLKEY_PK8"))
}

private fun readAndTeamLog(file: String) {
    teamLog.info("Reading file: $file")
    teamLog.info(File(file).readText(Charsets.UTF_8))
}
