package no.nav.tsm.sykinnapi.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.assertEquals
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tsm.sykinnapi.modell.Aktivitet
import no.nav.tsm.sykinnapi.modell.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.Sykmelding
import no.nav.tsm.sykinnapi.service.SykmeldingService
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableMockOAuth2Server
@WebMvcTest(SykmeldingApiController::class)
class SykmeldingApiControllerTest {

    @Autowired lateinit var mockMvc: MockMvc

    @MockBean lateinit var sykmeldingService: SykmeldingService

    @Test
    internal fun `Should return HttpStatus OK and body text ok`() {

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

        `when`(sykmeldingService.create(sykInnApiNySykmeldingPayload)).thenReturn(true)

        // TODO add i auth in request header
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/sykmelding/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ObjectMapper().writeValueAsString(sykInnApiNySykmeldingPayload)),
            )
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("ok")))
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
