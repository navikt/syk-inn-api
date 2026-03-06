package modules.behandler

import core.db.runFlywayMigrations
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import modules.sykmelder.configureSykmelderDependencies
import modules.sykmeldinger.configureSykmeldingerModule
import modules.sykmeldinger.db.exposed.SykmeldingJsonb
import no.nav.tsm.configureTestStuff
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.postgresql.PostgreSQLContainer
import utils.configureTestEnvironment

class BehandlerRoutesIntegrationTest {

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

        application {
            configureTestEnvironment()
            configureSykmelderDependencies()
            configureSykmeldingerModule()
            configureTestStuff()
        }

        client.post("/test/create-boio")
        client.post("/test/create-boio")
        client.post("/test/create-boio")

        val response = client.get("/test")
        assertEquals(HttpStatusCode.Created, response.status)

        val body = response.body<List<Pair<UUID, SykmeldingJsonb>>>()
        assertEquals(3, body.size)
    }
}
