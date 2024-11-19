package no.nav.tsm.sykinnapi.service.syfohelsenettproxy

import kotlin.test.assertEquals
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tsm.sykinnapi.client.SyfohelsenettproxyClient
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@EnableJwtTokenValidation
@EnableMockOAuth2Server
@SpringBootTest
class SyfohelsenettproxyServiceTest {

    @MockBean lateinit var syfohelsenettproxyClient: SyfohelsenettproxyClient

    val syfohelsenettproxyService: SyfohelsenettproxyService =
        SyfohelsenettproxyService(syfohelsenettproxyClient)

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

        assertEquals(sykmelderHpr, behandler.hprNummer)
    }
}
