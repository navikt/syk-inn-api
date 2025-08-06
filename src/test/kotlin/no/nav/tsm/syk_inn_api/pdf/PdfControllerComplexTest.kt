package no.nav.tsm.syk_inn_api.pdf

import no.nav.tsm.syk_inn_api.test.FullIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.util.*
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PdfControllerComplexTest(@param:Autowired private val restTemplate: TestRestTemplate) :
    FullIntegrationTest() {

    val baseUrl = "/api/sykmelding"

    @Test
    fun `should fail GET pdf when missing HPR value`() {
        // given
        val sykmeldingId = UUID.randomUUID()
        val headers =
            HttpHeaders().apply {
                set("HPR", "")
                accept = listOf(MediaType.APPLICATION_PDF, MediaType.TEXT_PLAIN)
            }

        // when
        val response =
            restTemplate.exchange<String>(
                "$baseUrl/$sykmeldingId/pdf",
                HttpMethod.GET,
                HttpEntity<Void>(null, headers),
            )

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Missing HPR parameter", response.body)
    }

    @Test
    fun `GET pdf should require HPR key`() {
        // given
        val sykmeldingId = UUID.randomUUID()
        val headers =
            HttpHeaders().apply { accept = listOf(MediaType.APPLICATION_PDF, MediaType.TEXT_PLAIN) }

        // when
        val response =
            restTemplate.exchange<String>(
                "$baseUrl/$sykmeldingId/pdf",
                HttpMethod.GET,
                HttpEntity<Void>(null, headers),
            )

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `HPR header should not be case sensitive`() {
        // given
        val sykmeldingId = UUID.randomUUID()
        val headers =
            HttpHeaders().apply { accept = listOf(MediaType.APPLICATION_PDF, MediaType.TEXT_PLAIN) }

        // when
        val response =
            restTemplate.exchange<String>(
                "$baseUrl/$sykmeldingId/pdf",
                HttpMethod.GET,
                HttpEntity<Void>(null, headers),
            )

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }
}
