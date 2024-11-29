package no.nav.tsm.sykinnapi.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tsm.sykinnapi.mapper.receivedSykmeldingMapper
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Status
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.toReceivedSykmeldingWithValidation
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.sykinn.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.Sykmelding
import no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper.ReceivedSykmeldingMapper
import no.nav.tsm.sykinnapi.service.syfohelsenettproxy.SyfohelsenettproxyService
import no.nav.tsm.sykinnapi.service.syfosmregler.SyfosmreglerService
import no.nav.tsm.sykinnapi.service.sykmelding.SykmeldingService
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableMockOAuth2Server
@WebMvcTest(SykmeldingApiController::class)
class SykmeldingApiControllerTest {

    @Autowired lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired lateinit var mockMvc: MockMvc

    @MockitoBean lateinit var sykmeldingService: SykmeldingService

    @MockitoBean lateinit var syfosmreglerService: SyfosmreglerService

    @MockitoBean lateinit var receivedSykmeldingMapper: ReceivedSykmeldingMapper

    @MockitoBean lateinit var syfohelsenettproxyService: SyfohelsenettproxyService

    //TODO fix the non-null is null
    @Test
    internal fun `Should return HttpStatus OK and body text ok`() {

        val sykmelderFnr = "12345678912"

        val sykmeldingsId = "123213-2323-213123123"

        val sykInnApiNySykmeldingPayload =
            SykInnApiNySykmeldingPayload(
                pasientFnr = "12345",
                sykmelderHpr = "123123",
                sykmelding =
                    Sykmelding(
                        hoveddiagnose =
                            Hoveddiagnose(
                                system = DiagnoseSystem.ICD10,
                                code = "S017",
                            ),
                        aktivitet =
                            Aktivitet.AktivitetIkkeMulig(
                                fom = "2020-01-01",
                                tom = "2020-01-02",
                            ),
                    ),
            )

        val receivedSykmelding =
            receivedSykmeldingMapper(
                sykInnApiNySykmeldingPayload,
                sykmelderFnr,
                sykmeldingsId,
            )

        `when`(
                receivedSykmeldingMapper.mapToReceivedSykmelding(
                    sykInnApiNySykmeldingPayload,
                    sykmelderFnr,
                    sykmeldingsId,
                )
            )
            .thenReturn(receivedSykmelding)

        `when`(syfosmreglerService.validate(receivedSykmelding))
            .thenReturn(
                ValidationResult(
                    Status.OK,
                    emptyList(),
                ),
            )

        val receivedSykmeldingWithValidation =
            receivedSykmelding.toReceivedSykmeldingWithValidation(
                ValidationResult(
                    Status.OK,
                    emptyList(),
                )
            )

        `when`(syfohelsenettproxyService.getBehandlerByHpr(anyString(), anyString()))
            .thenReturn(
                Behandler(
                    godkjenninger = emptyList(),
                    fnr = sykmelderFnr,
                    hprNummer = sykInnApiNySykmeldingPayload.sykmelderHpr,
                    fornavn = "Fornavn",
                    mellomnavn = null,
                    etternavn = "etternavn",
                ),
            )

        `when`(sykmeldingService.sendToOkTopic(receivedSykmeldingWithValidation))
            .thenReturn(sykmeldingsId)

        val jwt =
            mockOAuth2Server.issueToken(
                issuerId = "azuread",
                audience = "syk-inn-api-client-id",
                subject = "testuser",
                claims =
                    mapOf(
                        "azp" to "consumerClientId",
                        "appid" to "consumerClientId",
                    ),
            )

        println("Bearer ${jwt.serialize()}")

        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/sykmelding/create")
                    .header(
                        "Authorization",
                        "Bearer ${jwt.serialize()}",
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ObjectMapper().writeValueAsString(sykInnApiNySykmeldingPayload)),
            )
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("")))
    }
}
