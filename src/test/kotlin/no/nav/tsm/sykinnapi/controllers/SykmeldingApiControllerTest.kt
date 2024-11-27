package no.nav.tsm.sykinnapi.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.assertEquals
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.sykinn.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.Sykmelding
import no.nav.tsm.sykinnapi.service.syfohelsenettproxy.SyfohelsenettproxyService
import no.nav.tsm.sykinnapi.service.sykmelding.SykmeldingService
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert
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

    @MockitoBean lateinit var syfohelsenettproxyService: SyfohelsenettproxyService

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

        `when`(syfohelsenettproxyService.getBehandlerByHpr(anyString(), anyString()))
            .thenReturn(
                Behandler(
                    godkjenninger = emptyList(),
                    fnr = sykmelderFnr,
                    hprNummer = sykInnApiNySykmeldingPayload.sykmelderHpr,
                    fornavn = "Fornavn",
                    mellomnavn = null,
                    etternavn = "etternavn",
                )
            )

        `when`(sykmeldingService.create(sykInnApiNySykmeldingPayload, sykmelderFnr, sykmeldingsId))
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
                    )
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

    // TODO: Temporary test
    @Test
    internal fun `seralization of discriminating union should have a type discriminator`() {
        val mappedValue =
            ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(
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
                    ),
                )

        JSONAssert.assertEquals(
            mappedValue,
            """{
              "pasientFnr": "12345",
              "sykmelderHpr": "123123",
              "sykmelding": {
                "hoveddiagnose": {
                  "system": "ICD10",
                  "code": "S017"
                },
                "aktivitet": {
                  "type": "AKTIVITET_IKKE_MULIG",
                  "fom": "2020-01-01",
                  "tom": "2020-01-02"
                }
              }
            }""",
            false,
        )
    }

    // TODO: Temporary test
    @Test
    internal fun `de-seralization of discriminating union should work`() {
        val mapper =
            ObjectMapper()
                .registerModule(
                    KotlinModule.Builder().build(),
                )

        val gradert =
            mapper.readValue<SykInnApiNySykmeldingPayload>(
                """{
              "pasientFnr": "12345",
              "sykmelderHpr": "123123",
              "sykmelding": {
                "hoveddiagnose": {
                  "system": "ICD10",
                  "code": "S017"
                },
                "aktivitet": {
                  "type": "GRADERT",
                  "fom": "2020-01-01",
                  "tom": "2020-01-02",
                  "grad": 50
                }
              }
            }""",
            )

        assert(gradert.sykmelding.aktivitet is Aktivitet.Gradert)
        assertEquals((gradert.sykmelding.aktivitet as Aktivitet.Gradert).grad, 50)

        val aktivitetIkkeMulig =
            mapper.readValue<SykInnApiNySykmeldingPayload>(
                """{
              "pasientFnr": "12345",
              "sykmelderHpr": "123123",
              "sykmelding": {
                "hoveddiagnose": {
                  "system": "ICD10",
                  "code": "S017"
                },
                "aktivitet": {
                  "type": "AKTIVITET_IKKE_MULIG",
                  "fom": "2020-01-01",
                  "tom": "2020-01-02"
                }
              }
            }""",
            )

        assert(aktivitetIkkeMulig.sykmelding.aktivitet is Aktivitet.AktivitetIkkeMulig)
    }
}
