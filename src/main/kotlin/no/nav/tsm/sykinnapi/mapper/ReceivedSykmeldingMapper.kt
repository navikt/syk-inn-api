package no.nav.tsm.sykinnapi.mapper

import java.time.LocalDateTime
import java.time.ZoneOffset
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.util.fellesformatMarshaller
import no.nav.tsm.sykinnapi.util.toString

fun receivedSykmeldingMapper(
    sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload,
    sykmelderFnr: String,
    sykmeldingId: String
): ReceivedSykmelding {
    val now = LocalDateTime.now(ZoneOffset.UTC)

    val fellesformat =
        mapToFellesformat(
            sykmelderHpr = sykInnApiNySykmeldingPayload.sykmelderHpr,
            sykmeldingId = sykmeldingId,
            pasientfnr = sykInnApiNySykmeldingPayload.pasientFnr,
            hoveddiagnose = sykInnApiNySykmeldingPayload.sykmelding.hoveddiagnose,
            sykInnAktivitet = sykInnApiNySykmeldingPayload.sykmelding.aktivitet,
            now = now,
        )

    val sykmelding =
        extractHelseOpplysningerArbeidsuforhet(fellesformat)
            .toSykmelding(
                sykmeldingId = sykmeldingId,
                pasientAktoerId = "",
                msgId = sykmeldingId,
                signaturDato = now,
            )

    val receivedSykmelding =
        ReceivedSykmelding(
            sykmelding = sykmelding,
            personNrPasient = sykInnApiNySykmeldingPayload.pasientFnr,
            tlfPasient = null,
            personNrLege = sykmelderFnr,
            legeHprNr = sykInnApiNySykmeldingPayload.sykmelderHpr,
            legeHelsepersonellkategori = null,
            navLogId = sykmeldingId,
            msgId = sykmeldingId,
            legekontorOrgNr = null,
            legekontorOrgName = "",
            legekontorHerId = null,
            legekontorReshId = null,
            mottattDato = now,
            rulesetVersion = null,
            fellesformat = fellesformatMarshaller.toString(fellesformat),
            tssid = null,
            merknader = null,
            partnerreferanse = null,
            vedlegg = emptyList(),
            utenlandskSykmelding = null,
        )

    return receivedSykmelding
}
