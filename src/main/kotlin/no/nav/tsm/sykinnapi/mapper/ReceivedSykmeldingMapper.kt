package no.nav.tsm.sykinnapi.mapper

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import no.nav.tsm.sykinnapi.modell.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmeldingWithValidation
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Status
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import no.nav.tsm.sykinnapi.util.fellesformatMarshaller
import no.nav.tsm.sykinnapi.util.toString

fun receivedSykmeldingWithValidationMapper(
    sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload
): ReceivedSykmeldingWithValidation {
    val sykmeldingId = UUID.randomUUID().toString()
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

    val receivedSykmeldingWithValidation =
        ReceivedSykmeldingWithValidation(
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
            mottattDato = now,
            rulesetVersion = null,
            fellesformat = fellesformatMarshaller.toString(fellesformat),
            tssid = null,
            merknader = null,
            partnerreferanse = null,
            vedlegg = emptyList(),
            utenlandskSykmelding = null,
            validationResult = ValidationResult(Status.OK, emptyList())
        )

    return receivedSykmeldingWithValidation
}
