package modules.sykmeldinger

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.*
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import no.nav.tsm.core.db.runFlywayMigrations
import no.nav.tsm.modules.sykmeldinger.configureSykmeldingerApi
import no.nav.tsm.modules.sykmeldinger.db.exposed.SykmeldingJsonb
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.postgresql.PostgreSQLContainer

class SykmeldingRoutesIntegrationTest {

    val postgres = PostgreSQLContainer("postgres:17-alpine")

    init {
        postgres.start()

        runFlywayMigrations(
            postgres.jdbcUrl,
            user = postgres.username,
            password = postgres.password,
        )
        Database.connect(postgres.jdbcUrl, user = postgres.username, password = postgres.password)
    }

    @Test
    fun `example integration test`() = testApplication {
        client = createClient { install(ContentNegotiation) { jackson() } }

        application { configureSykmeldingerApi() }

        client.post("/create-boio")
        client.post("/create-boio")
        client.post("/create-boio")

        val response = client.get("/test")
        assertEquals(HttpStatusCode.Created, response.status)

        val body = response.body<List<Pair<UUID, SykmeldingJsonb>>>()
        assertEquals(3, body.size)
    }
}
