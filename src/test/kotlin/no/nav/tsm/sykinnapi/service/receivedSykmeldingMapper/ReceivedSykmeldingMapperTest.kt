package no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.AvsenderSystem
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.KontaktMedPasient
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.sykinn.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.Sykmelding
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReceivedSykmeldingMapperTest {
    lateinit var receivedSykmeldingMapper: ReceivedSykmeldingMapper

    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        receivedSykmeldingMapper = ReceivedSykmeldingMapper(objectMapper)
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
                sykmeldingsId,
            )

        assertEquals(sykmeldingsId, receivedSykmelding.msgId)
        assertEquals(sykmeldingsId, receivedSykmelding.sykmelding.id)
        assertEquals(
            sykInnApiNySykmeldingPayload.sykmelding.hoveddiagnose.code,
            receivedSykmelding.sykmelding.medisinskVurdering.hovedDiagnose?.kode,
        )
        assertEquals("12345", receivedSykmelding.personNrPasient)
        assertEquals(null, receivedSykmelding.tssid)
        assertEquals(null, receivedSykmelding.legekontorOrgNr)
        assertEquals("", receivedSykmelding.sykmelding.pasientAktoerId)
        assertNotNull(receivedSykmelding.sykmelding.medisinskVurdering)
        assertEquals(false, receivedSykmelding.sykmelding.skjermesForPasient)
        assertNotNull(receivedSykmelding.sykmelding.arbeidsgiver)
        assertEquals(1, receivedSykmelding.sykmelding.perioder.size)
        assertEquals(null, receivedSykmelding.sykmelding.prognose)
        assertEquals(emptyMap(), receivedSykmelding.sykmelding.utdypendeOpplysninger)
        assertEquals(null, receivedSykmelding.sykmelding.tiltakArbeidsplassen)
        assertEquals(null, receivedSykmelding.sykmelding.tiltakNAV)
        assertEquals(null, receivedSykmelding.sykmelding.andreTiltak)
        assertEquals(null, receivedSykmelding.sykmelding.meldingTilNAV?.bistandUmiddelbart)
        assertEquals(null, receivedSykmelding.sykmelding.meldingTilArbeidsgiver)
        assertEquals(KontaktMedPasient(null, null), receivedSykmelding.sykmelding.kontaktMedPasient)
        assertEquals(
            LocalDateTime.of(LocalDate.now(), LocalTime.NOON).year,
            receivedSykmelding.sykmelding.behandletTidspunkt.year
        )
        assertEquals(
            LocalDateTime.of(LocalDate.now(), LocalTime.NOON).month,
            receivedSykmelding.sykmelding.behandletTidspunkt.month
        )
        assertEquals(
            LocalDateTime.of(LocalDate.now(), LocalTime.NOON).dayOfMonth,
            receivedSykmelding.sykmelding.behandletTidspunkt.dayOfMonth
        )
        assertNotNull(receivedSykmelding.sykmelding.behandler)
        assertEquals(
            AvsenderSystem("syk-inn", "1.0.0"),
            receivedSykmelding.sykmelding.avsenderSystem
        )
        assertEquals(LocalDate.now(), receivedSykmelding.sykmelding.syketilfelleStartDato)
        assertEquals(
            LocalDateTime.of(LocalDate.now(), LocalTime.NOON).year,
            receivedSykmelding.sykmelding.signaturDato.year
        )
        assertEquals(
            LocalDateTime.of(LocalDate.now(), LocalTime.NOON).month,
            receivedSykmelding.sykmelding.signaturDato.month
        )
        assertEquals(
            LocalDateTime.of(LocalDate.now(), LocalTime.NOON).dayOfMonth,
            receivedSykmelding.sykmelding.signaturDato.dayOfMonth
        )
        assertEquals(null, receivedSykmelding.sykmelding.navnFastlege)
        assertEquals(null, receivedSykmelding.legeHelsepersonellkategori)
    }
}
