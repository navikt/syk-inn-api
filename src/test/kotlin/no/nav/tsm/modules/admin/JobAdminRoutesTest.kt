package no.nav.tsm.modules.admin

import io.kotest.matchers.equals.shouldEqual
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.testing.*
import kotlin.test.Test
import no.nav.tsm.modules.admin.db.JobRepository
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.poison.PoisonedSykmelding
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.poison.SykmeldingPoisonPillRepo
import no.nav.tsm.utils.WithPostgresql
import no.nav.tsm.utils.configurePostgresIntegrationTests
import no.nav.tsm.utils.testClient

class JobAdminRoutesTest : WithPostgresql() {

    private fun ApplicationTestBuilder.configureAdminRoutesTest() {
        client = testClient()

        application {
            configurePostgresIntegrationTests(postgres)

            // Bare minimum needed to test admin routes
            dependencies { provide(SykmeldingPoisonPillRepo::class) }
            dependencies { provide(JobRepository::class) }

            // Modules in test
            configureJobAdminRoutes()
        }

        runMigrations(true)
        connect()
    }

    @Test
    fun `poison pills - should be able to insert poison pill with reason`() = testApplication {
        configureAdminRoutesTest()

        val response =
            client.put("/internal/admin/poison-pills/8bb6fddc-2f59-4439-9e75-b4ddebe0d276") {
                headers { append("Content-Type", "application/json") }
                setBody("""{"reason": "test reason"}""")
            }

        response.status shouldBe HttpStatusCode.OK
        val body = response.body<PoisonedSykmelding>()

        body.id.toString() shouldEqual "8bb6fddc-2f59-4439-9e75-b4ddebe0d276"
        body.reason shouldBe "test reason, by local-symfoni-user"
    }
}
