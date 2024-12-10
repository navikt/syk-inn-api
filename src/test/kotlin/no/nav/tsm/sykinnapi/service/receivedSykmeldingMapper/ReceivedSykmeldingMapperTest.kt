package no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import no.nav.tsm.sykinnapi.mapper.receivedSykmeldingMapper
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.AvsenderSystem
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.KontaktMedPasient
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet.*
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem.*
import no.nav.tsm.sykinnapi.modell.sykinn.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.Sykmelding
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import kotlin.test.assertNull

@JsonTest
import org.springframework.boot.test.context.SpringBootTest

class ReceivedSykmeldingMapperTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    internal fun `Should map correctly`() {
        val mapper = ReceivedSykmeldingMapper(objectMapper)
        val fnr = "1344333"
        val id = "123213-2323-213123123"
        val payload = SykInnApiNySykmeldingPayload("12345", "123123", Sykmelding(Hoveddiagnose(ICD10, "S017"), AktivitetIkkeMulig("2020-01-01", "2020-01-02")))

        with(mapper.mapToReceivedSykmelding(payload,fnr,id)) {
            assertEquals(id, msgId)
            assertEquals(id, sykmelding.id)
            assertEquals("12345", personNrPasient)
            assertNull(tssid)
            assertNull(legekontorOrgNr)
            assertNull(legeHelsepersonellkategori)
            with(sykmelding) {
                assertEquals("", pasientAktoerId)
                assertNotNull(medisinskVurdering)
                assertThat(skjermesForPasient).isFalse
                assertNotNull(arbeidsgiver)
                assertEquals(1, perioder.size)
                assertNull(prognose)
                assertThat(utdypendeOpplysninger.isEmpty())
                assertNull(tiltakArbeidsplassen)
                assertNull(tiltakNAV)
                assertNull(andreTiltak)
                assertNull(meldingTilNAV?.bistandUmiddelbart)
                assertNull(meldingTilArbeidsgiver)
                assertEquals(payload.sykmelding.hoveddiagnose.code, medisinskVurdering.hovedDiagnose?.kode, )
                assertEquals(KontaktMedPasient(null, null), kontaktMedPasient)
                assertEquals(LocalDateTime.of(LocalDate.now(), LocalTime.NOON).year, behandletTidspunkt.year)
                assertEquals(LocalDateTime.of(LocalDate.now(), LocalTime.NOON).month, behandletTidspunkt.month)
                assertEquals(LocalDateTime.of(LocalDate.now(), LocalTime.NOON).dayOfMonth, behandletTidspunkt.dayOfMonth)
                assertNotNull(behandler)
                assertEquals(AvsenderSystem("syk-inn", "1.0.0"), avsenderSystem)
                assertEquals(LocalDate.now(), syketilfelleStartDato)
                assertEquals(LocalDateTime.of(LocalDate.now(), LocalTime.NOON).year, signaturDato.year)
                assertEquals(LocalDateTime.of(LocalDate.now(), LocalTime.NOON).month, signaturDato.month)
                assertEquals(LocalDateTime.of(LocalDate.now(), LocalTime.NOON).dayOfMonth, signaturDato.dayOfMonth)
                assertNull(navnFastlege)
            }
        }
    }
}
