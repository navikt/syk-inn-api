package no.nav.tsm.sykinnapi.client.syfosmregler

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import kotlin.test.assertEquals
import no.nav.tsm.sykinnapi.mapper.receivedSykmeldingMapper
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Status
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet.AktivitetIkkeMulig
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem.ICD10
import no.nav.tsm.sykinnapi.modell.sykinn.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.Sykmelding
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.HttpMethod.*
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.*
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.*

@RestClientTest(SyfosmreglerClient::class)
class SyfosmreglerClientTest(@Value("\${syfosmregler.url}") private val baseUrl: String) {
    @Autowired private lateinit var mockRestServiceServer: MockRestServiceServer

    @Autowired private lateinit var syfosmreglerClient: SyfosmreglerClient

    @Autowired private lateinit var objectMapper: ObjectMapper

    @Test
    internal fun `Should return validationResult status OK`() {

        val sykmelderFnr = "12345678912"
        val sykmeldingsId = "123213-2323-213123123"

        val sykInnApiNySykmeldingPayload =
            SykInnApiNySykmeldingPayload(
                "12345",
                "123123",
                Sykmelding(
                    Hoveddiagnose(ICD10, "S017"),
                    AktivitetIkkeMulig("2020-01-01", "2020-01-02")
                )
            )

        val receivedSykmelding =
            receivedSykmeldingMapper(sykInnApiNySykmeldingPayload, sykmelderFnr, sykmeldingsId)

        val response =
            objectMapper.writeValueAsString(
                ValidationResult(
                    Status.OK,
                    emptyList(),
                ),
            )

        mockRestServiceServer
            .expect(requestTo(URI("$baseUrl/v1/rules/validate")))
            .andExpect(method(POST))
            .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(response))

        val validationResult = syfosmreglerClient.validate(receivedSykmelding)

        assertEquals(Status.OK, validationResult.status)
    }
}
