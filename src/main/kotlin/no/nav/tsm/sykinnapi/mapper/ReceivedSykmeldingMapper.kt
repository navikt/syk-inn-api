package no.nav.tsm.sykinnapi.mapper

import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.syfosmregister.SykInnSykmeldingDTO
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.sykinn.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

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

fun mapToSystem(system: String): DiagnoseSystem {
    return when (system) {
        "2.16.578.1.12.4.1.1.7110" -> DiagnoseSystem.ICD10
        "2.16.578.1.12.4.1.1.7170" -> DiagnoseSystem.ICPC2
        else -> throw RuntimeException("Ukjent system: $system")
    }
}

fun receivedSykmeldingMapper(
    sykInnSykmeldingDTO: SykInnSykmeldingDTO,
    sykmelderFnr: String,
    sykmeldingId: String
): ReceivedSykmelding {
    val now = LocalDateTime.now(ZoneOffset.UTC)

    val fellesformat =
        mapToFellesformat(
            sykmelderHpr = sykInnSykmeldingDTO.behandler.hpr!!,
            sykmeldingId = sykmeldingId,
            pasientfnr = sykInnSykmeldingDTO.pasient.fnr,
            hoveddiagnose =
                Hoveddiagnose(
                    code = sykInnSykmeldingDTO.hovedDiagnose.code,
                    system = mapToSystem(sykInnSykmeldingDTO.hovedDiagnose.system)
                ),
            sykInnAktivitet =
                when (sykInnSykmeldingDTO.aktivitet) {
                    is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.Gradert ->
                        Aktivitet.Gradert(
                            grad = sykInnSykmeldingDTO.aktivitet.grad,
                            fom =
                                sykInnSykmeldingDTO.aktivitet.fom.format(
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.of("nb", "NO"))
                                ),
                            tom =
                                sykInnSykmeldingDTO.aktivitet.tom.format(
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.of("nb", "NO"))
                                )
                        )
                    is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.AktivitetIkkeMulig ->
                        Aktivitet.AktivitetIkkeMulig(
                            fom =
                                sykInnSykmeldingDTO.aktivitet.fom.format(
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.of("nb", "NO"))
                                ),
                            tom =
                                sykInnSykmeldingDTO.aktivitet.tom.format(
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.of("nb", "NO"))
                                )
                        )
                    is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.Avvetende -> TODO()
                    is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.Behandlingsdager ->
                        TODO()
                    is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.Reisetilskudd -> TODO()
                },
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
            personNrPasient = sykInnSykmeldingDTO.pasient.fnr,
            tlfPasient = null,
            personNrLege = sykmelderFnr,
            legeHprNr = sykInnSykmeldingDTO.behandler.hpr,
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
