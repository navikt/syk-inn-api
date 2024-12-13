package no.nav.tsm.sykinnapi.service.syfohelsenettproxy

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlin.test.assertEquals
import no.nav.tsm.sykinnapi.client.syfohelsenettproxy.SyfohelsenettproxyClient
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SyfohelsenettproxyServiceTest {

    @MockK lateinit var syfohelsenettproxyClient: SyfohelsenettproxyClient

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

        every { syfohelsenettproxyClient.getBehandlerByHpr(sykmelderHpr, sykmeldingsId) } returns
            Behandler(
                godkjenninger = emptyList(),
                fnr = sykmelderFnr,
                hprNummer = sykmelderHpr,
                fornavn = "Fornavn",
                mellomnavn = null,
                etternavn = "etternavn",
            )

        val behandler = syfohelsenettproxyService.getBehandlerByHpr(sykmelderHpr, sykmeldingsId)

        assertEquals(sykmelderHpr, behandler.hprNummer)
    }
}
