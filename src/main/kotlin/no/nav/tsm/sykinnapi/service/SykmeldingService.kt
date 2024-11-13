package no.nav.tsm.sykinnapi.service

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import no.nav.tsm.sykinnapi.modell.Aktivitet
import no.nav.tsm.sykinnapi.modell.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Adresse
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Arbeidsgiver
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.AvsenderSystem
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Behandler
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Diagnose
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Gradert
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.HarArbeidsgiver
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.KontaktMedPasient
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.MedisinskVurdering
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Periode
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Sykmelding
import org.springframework.stereotype.Service

@Service
class SykmeldingService {
    fun create(sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload): String {
        //  TODO pu on kafka topic

        val sykmeldingId = UUID.randomUUID().toString()
        val now = LocalDateTime.now(ZoneOffset.UTC)

        val sykmelding =
            Sykmelding(
                id = sykmeldingId,
                msgId = sykmeldingId,
                pasientAktoerId = "",
                medisinskVurdering =
                    MedisinskVurdering(
                        hovedDiagnose =
                            sykInnApiNySykmeldingPayload.sykmelding.hoveddiagnose.toDiagnose(),
                        biDiagnoser = emptyList(),
                        svangerskap = false,
                        yrkesskade = false,
                        yrkesskadeDato = null,
                        annenFraversArsak = null
                    ),
                skjermesForPasient = false,
                arbeidsgiver = Arbeidsgiver(HarArbeidsgiver.INGEN_ARBEIDSGIVER, null, null, null),
                perioder =
                    toPerioder(aktivitet = sykInnApiNySykmeldingPayload.sykmelding.aktivitet),
                prognose = null,
                utdypendeOpplysninger = mapOf(),
                tiltakArbeidsplassen = null,
                tiltakNAV = null,
                andreTiltak = null,
                meldingTilNAV = null,
                meldingTilArbeidsgiver = null,
                kontaktMedPasient = KontaktMedPasient(null, null),
                behandletTidspunkt = now,
                behandler =
                    Behandler(
                        fornavn = "",
                        mellomnavn = null,
                        etternavn = "",
                        aktoerId = "",
                        fnr = "TODO",
                        hpr = sykInnApiNySykmeldingPayload.sykmelderHpr,
                        her = null,
                        adresse = Adresse(null, null, null, null, null),
                        tlf = null,
                    ),
                avsenderSystem =
                    AvsenderSystem(
                        navn = "syk-inn",
                        versjon = "1.0.0",
                    ),
                syketilfelleStartDato = null,
                signaturDato = now,
                navnFastlege = null,
            )

        val receivedSykmelding =
            ReceivedSykmelding(
                sykmelding = sykmelding,
                personNrPasient = sykInnApiNySykmeldingPayload.pasientFnr,
                tlfPasient = null,
                personNrLege = "TODO",
                legeHprNr = sykInnApiNySykmeldingPayload.sykmelderHpr,
                legeHelsepersonellkategori = null,
                navLogId = sykmeldingId,
                msgId = sykmeldingId,
                legekontorOrgNr = null,
                legekontorOrgName = "",
                legekontorHerId = null,
                legekontorReshId = null,
                mottattDato =
                    LocalDateTime.now()
                        .atZone(ZoneId.systemDefault())
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime(),
                rulesetVersion = null,
                fellesformat = "TODO",
                tssid = "",
                merknader = null,
                partnerreferanse = null,
                vedlegg = emptyList(),
                utenlandskSykmelding = null,
            )

        return receivedSykmelding.sykmelding.id
    }
}

private fun Hoveddiagnose.toDiagnose(): Diagnose {
    return Diagnose(
        system =
            when (system) {
                DiagnoseSystem.ICD10 -> "2.16.578.1.12.4.1.1.7110"
                DiagnoseSystem.ICPC2 -> "2.16.578.1.12.4.1.1.7170"
            },
        kode = code,
        tekst = ""
    )
}

private fun toLocalDate(dateString: String): LocalDate {
    val formatterDateIso = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.of("nb", "NO"))
    return LocalDate.parse(dateString, formatterDateIso)
}

private fun toPerioder(aktivitet: Aktivitet): List<Periode> {

    when (aktivitet) {
        is Aktivitet.Gradert -> {
            return listOf(
                Periode(
                    fom = toLocalDate(aktivitet.fom),
                    tom = toLocalDate(aktivitet.tom),
                    aktivitetIkkeMulig = null,
                    avventendeInnspillTilArbeidsgiver = null,
                    behandlingsdager = null,
                    gradert = Gradert(reisetilskudd = false, grad = aktivitet.grad),
                    reisetilskudd = false
                )
            )
        }
        is Aktivitet.AktivitetIkkeMulig -> {
            return listOf(
                Periode(
                    fom = toLocalDate(aktivitet.fom),
                    tom = toLocalDate(aktivitet.tom),
                    aktivitetIkkeMulig = null,
                    avventendeInnspillTilArbeidsgiver = null,
                    behandlingsdager = null,
                    gradert = null,
                    reisetilskudd = false
                )
            )
        }
        else -> {
            throw IllegalArgumentException("Invalid Aktivitet")
        }
    }
}
