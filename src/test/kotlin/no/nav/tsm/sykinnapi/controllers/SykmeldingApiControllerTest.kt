package no.nav.tsm.sykinnapi.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.junit5.MockKExtension
import no.nav.tsm.sykinnapi.mapper.receivedSykmeldingMapper
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.toReceivedSykmeldingWithValidation
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet.AktivitetIkkeMulig
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem.*
import no.nav.tsm.sykinnapi.modell.sykinn.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.Sykmelding
import no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper.ReceivedSykmeldingMapper
import no.nav.tsm.sykinnapi.service.syfohelsenettproxy.SyfohelsenettproxyService
import no.nav.tsm.sykinnapi.service.syfosmregler.SyfosmreglerService
import no.nav.tsm.sykinnapi.service.sykmelding.SykmeldingService
import no.nav.tsm.sykinnapi.service.sykmeldingInnsending.SykmeldingInnsending
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.MediaType.*
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.BeforeTest

@WebMvcTest(SykmeldingApiController::class)
@Import(OAuth2ClientAutoConfiguration::class)
class SykmeldingApiControllerTest {

    @TestConfiguration
    class TestConfig {

       // @Bean
        fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
            http {
                oauth2ResourceServer {
                    jwt {
                    }
                }
                oauth2Client {
                }
                authorizeHttpRequests {
                    authorize("/internal/health/**", permitAll)
                    authorize(anyRequest, authenticated)
                }
            }
            return http.build()
        }
        @Bean
        fun sykmeldingInnsending(
            sykmelding: SykmeldingService,
            regler: SyfosmreglerService,
            mapper: ReceivedSykmeldingMapper,
            proxy: SyfohelsenettproxyService,
            objectMapper: ObjectMapper) =
            SykmeldingInnsending(sykmelding, proxy, regler, mapper,objectMapper)
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var sykmelding: SykmeldingService

    @MockkBean
    lateinit var regler: SyfosmreglerService

    @MockkBean
    lateinit var mapper: ReceivedSykmeldingMapper

    @MockkBean
    lateinit var proxy: SyfohelsenettproxyService

    @Autowired
    lateinit var innsender: SykmeldingInnsending

    val payload = SykInnApiNySykmeldingPayload("12345", "123123", Sykmelding(Hoveddiagnose(ICD10, "S017"), AktivitetIkkeMulig("2020-01-01", "2020-01-02" )))


    @BeforeTest
    fun setup() {
        val sykmelderFnr = "12345678912"
        val sykmeldingsId = "123213-2323-213123123"

        val receivedSykmelding = receivedSykmeldingMapper(payload, sykmelderFnr, sykmeldingsId)
        val receivedSykmeldingWithValidation = receivedSykmelding.toReceivedSykmeldingWithValidation(ValidationResult.OK)

        every { regler.validate(receivedSykmelding) } returns ValidationResult.OK
        every { mapper.mapToReceivedSykmelding(any(),any(),any()) } returns receivedSykmelding
        every { mapper.mapToReceivedSykmeldingWithValidationResult(receivedSykmelding, ValidationResult.OK) } returns receivedSykmeldingWithValidation
        every {proxy.getBehandlerByHpr(any(), any())  } returns Behandler(emptyList(), sykmelderFnr, payload.sykmelderHpr, "Fornavn", null, "etternavn")
        every {sykmelding.sendToOkTopic(receivedSykmeldingWithValidation)} returns sykmeldingsId
    }

    @Test
    @DisplayName("Should return HttpStatus OK and body text ok")
    internal fun ok() {

        mockMvc
            .perform(
                post("/api/v1/sykmelding/create")
                    .with(SecurityMockMvcRequestPostProcessors.jwt())
            .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isOk)
    }

    @Test
    @DisplayName("Should fail if no token")
    internal fun fail() {
        mockMvc
            .perform(
                post("/api/v1/sykmelding/create")
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isForbidden)
    }
}


