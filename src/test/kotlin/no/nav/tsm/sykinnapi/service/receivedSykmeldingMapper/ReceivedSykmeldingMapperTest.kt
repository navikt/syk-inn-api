package no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.sykinn.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.Sykmelding
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReceivedSykmeldingMapperTest {
    lateinit var receivedSykmeldingMapper: ReceivedSykmeldingMapper

    @BeforeEach
    fun setup() {
        receivedSykmeldingMapper = ReceivedSykmeldingMapper()
    }

    @Test
    internal fun `Should map correctly`() {

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

        val receivedSykmelding =
            receivedSykmeldingMapper.mapToReceivedSykmelding(
                sykInnApiNySykmeldingPayload,
                sykmelderFnr,
                sykmeldingsId
            )

        println(
            ObjectMapper()
                .apply {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                }
                .writeValueAsString(receivedSykmelding)
        )
    }
}
