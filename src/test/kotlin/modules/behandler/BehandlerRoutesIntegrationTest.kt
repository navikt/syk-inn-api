package modules.behandler

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import modules.sykmeldinger.configureSykmeldingerModule
import modules.sykmeldinger.db.exposed.SykmeldingJsonb
import no.nav.tsm.configureTestStuff
import utils.WithPostgresql
import utils.configureIntegrationTestDependencies

class BehandlerRoutesIntegrationTest : WithPostgresql() {

    @Test
    fun `example integration test`() = testApplication {
        client = createClient { install(ContentNegotiation) { jackson() } }

        application {
            configureIntegrationTestDependencies(postgres)
            configureSykmeldingerModule()
            configureBehandlerModule()
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
