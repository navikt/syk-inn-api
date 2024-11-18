package no.nav.tsm.sykinnapi.service.sykmelding

import kotlin.test.assertTrue
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.sykinn.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.Sykmelding
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@EnableJwtTokenValidation
@EnableMockOAuth2Server
@SpringBootTest
class SykmeldingServiceTest {

    val sykmeldingService: SykmeldingService = SykmeldingService()

    @Test
    internal fun `Should return sykmeldingId true when valid payload`() {

        val sykmelderFnr = "1344333"

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

        val sykmeldingId =
            sykmeldingService.create(sykInnApiNySykmeldingPayload, sykmelderFnr, sykmeldingsId)

        assertTrue(sykmeldingId.isNotBlank())
    }
}
