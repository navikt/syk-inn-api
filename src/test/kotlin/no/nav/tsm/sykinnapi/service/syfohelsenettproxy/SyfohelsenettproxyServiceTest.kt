package no.nav.tsm.sykinnapi.service.syfohelsenettproxy

import kotlin.test.assertEquals
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tsm.sykinnapi.client.syfohelsenettproxy.SyfohelsenettproxyClient
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@EnableJwtTokenValidation
@EnableMockOAuth2Server
@SpringBootTest
class SyfohelsenettproxyServiceTest {

    @MockitoBean lateinit var syfohelsenettproxyClient: SyfohelsenettproxyClient

    lateinit var syfohelsenettproxyService: SyfohelsenettproxyService

    @BeforeEach
    fun setup() {
        syfohelsenettproxyService = SyfohelsenettproxyService(syfohelsenettproxyClient)
    }

    @Test
    internal fun `Should return correct hprNummer`() {
        val sykmelderFnr = "32342244"
        val sykmelderHpr = "1344333"
        val sykmeldingsId = "123213-2323-213123123"

        `when`(syfohelsenettproxyClient.getBehandlerByHpr(anyString(), anyString()))
            .thenReturn(
                Behandler(
                    godkjenninger = emptyList(),
                    fnr = sykmelderFnr,
                    hprNummer = sykmelderHpr,
                    fornavn = "Fornavn",
                    mellomnavn = null,
                    etternavn = "etternavn",
                )
            )

        val behandler = syfohelsenettproxyService.getBehandlerByHpr(sykmelderHpr, sykmeldingsId)

        assertEquals(sykmelderHpr, behandler?.hprNummer)
    }
}
