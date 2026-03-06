package modules.sykmeldinger.rules.mappers

import java.time.LocalDateTime
import modules.behandler.payloads.SykInnDiagnoseSystem
import modules.sykmelder.Sykmelder
import modules.sykmelder.clients.pdl.IDENT_GRUPPE
import modules.sykmelder.clients.pdl.PdlPerson
import modules.sykmeldinger.domain.SykInnAktivitet
import modules.sykmeldinger.domain.SykInnDiagnoseInfo
import modules.sykmeldinger.domain.UnruledSykInnSykmelding
import no.nav.tsm.diagnoser.ICD10
import no.nav.tsm.diagnoser.ICPC2
import no.nav.tsm.diagnoser.ICPC2B
import no.nav.tsm.regulus.regula.RegulaAvsender
import no.nav.tsm.regulus.regula.RegulaBehandler
import no.nav.tsm.regulus.regula.RegulaMeta
import no.nav.tsm.regulus.regula.RegulaPasient
import no.nav.tsm.regulus.regula.RegulaPayload
import no.nav.tsm.regulus.regula.payload.Aktivitet
import no.nav.tsm.regulus.regula.payload.Diagnose

fun Sykmelder.mapSykmelderToRegulaBehandler(legekontorOrgnummer: String): RegulaBehandler =
    when (this) {
        is Sykmelder.FinnesIkke ->
            RegulaBehandler.FinnesIkke(
                // TODO: Fix this quirk in regulus-regula
                fnr = ""
            )

        is Sykmelder.UtenSuspensjon ->
            RegulaBehandler.FinnesIkke(
                // TODO: Fix this quirk in regulus-regula
                fnr = this.ident
            )

        is Sykmelder.MedSuspensjon ->
            RegulaBehandler.Finnes(
                suspendert = this.suspendert,
                // TODO: Provide
                godkjenninger = emptyList(),
                legekontorOrgnr = legekontorOrgnummer,
                fnr = this.ident,
            )
    }

fun PdlPerson.mapPdlPersonToRegulaPasient(): RegulaPasient? {
    val folkeregisterIdent =
        this.identer.firstOrNull { it.gruppe == IDENT_GRUPPE.FOLKEREGISTERIDENT }
    if (folkeregisterIdent == null) return null
    if (this.foedselsdato == null) return null

    return RegulaPasient(ident = folkeregisterIdent.ident, fodselsdato = this.foedselsdato)
}

fun mapUnruledSykInnSykmeldingToRegulaPayload(
    behandletTidspunkt: LocalDateTime,
    sykmelding: UnruledSykInnSykmelding,
    behandler: RegulaBehandler,
    pasient: RegulaPasient,
): RegulaPayload {
    val avsender =
        if (behandler is RegulaBehandler.Finnes) RegulaAvsender.Finnes(fnr = behandler.fnr)
        else RegulaAvsender.IngenAvsender

    return RegulaPayload(
        sykmeldingId = sykmelding.sykmeldingId.toString(),
        meta = RegulaMeta.Meta(sendtTidspunkt = LocalDateTime.now()),
        pasient = pasient,
        behandler = behandler,
        avsender = avsender,
        hoveddiagnose =
            sykmelding.values.hoveddiagnose.let { diagnose ->
                Diagnose(
                    kode = diagnose.code,
                    system =
                        when (diagnose.system) {
                            SykInnDiagnoseSystem.ICPC2 -> ICPC2.OID
                            SykInnDiagnoseSystem.ICD10 -> ICD10.OID
                            SykInnDiagnoseSystem.ICPC2B -> ICPC2B.OID
                        },
                )
            },
        bidiagnoser = mapToRegulaBidiagnoser(sykmelding.values.bidiagnoser),
        aktivitet = mapToSykmeldingAktivitet(sykmelding.values.aktivitet.first()),
        // TODO: Provide
        annenFravarsArsak = null,
        // TODO: Provide
        utdypendeOpplysninger = emptyMap(),
        // TODO Provide
        tidligereSykmeldinger = emptyList(),
        kontaktPasientBegrunnelseIkkeKontakt = sykmelding.values.tilbakedatering?.begrunnelse,
        behandletTidspunkt = behandletTidspunkt,
    )
}

private fun mapToRegulaBidiagnoser(bidiagnoser: List<SykInnDiagnoseInfo>?): List<Diagnose>? {
    if (bidiagnoser.isNullOrEmpty()) {
        return null
    }

    return bidiagnoser.map { diagnose ->
        Diagnose(
            kode = diagnose.code,
            system =
                when (diagnose.system) {
                    SykInnDiagnoseSystem.ICPC2 -> ICPC2.OID
                    SykInnDiagnoseSystem.ICD10 -> ICD10.OID
                    SykInnDiagnoseSystem.ICPC2B -> ICPC2B.OID
                },
        )
    }
}

private fun mapToSykmeldingAktivitet(opprettSykmeldingAktivitet: SykInnAktivitet): List<Aktivitet> {
    return listOf(
        when (opprettSykmeldingAktivitet) {
            is SykInnAktivitet.IkkeMulig ->
                Aktivitet.IkkeMulig(
                    fom = opprettSykmeldingAktivitet.fom,
                    tom = opprettSykmeldingAktivitet.tom,
                )

            is SykInnAktivitet.Gradert ->
                Aktivitet.Gradert(
                    fom = opprettSykmeldingAktivitet.fom,
                    tom = opprettSykmeldingAktivitet.tom,
                    grad = opprettSykmeldingAktivitet.grad,
                )

            is SykInnAktivitet.Avventende ->
                Aktivitet.Avventende(
                    avventendeInnspillTilArbeidsgiver =
                        opprettSykmeldingAktivitet.innspillTilArbeidsgiver,
                    fom = opprettSykmeldingAktivitet.fom,
                    tom = opprettSykmeldingAktivitet.tom,
                )

            is SykInnAktivitet.Behandlingsdager ->
                Aktivitet.Behandlingsdager(
                    behandlingsdager = opprettSykmeldingAktivitet.antallBehandlingsdager,
                    fom = opprettSykmeldingAktivitet.fom,
                    tom = opprettSykmeldingAktivitet.tom,
                )

            is SykInnAktivitet.Reisetilskudd ->
                Aktivitet.Reisetilskudd(
                    fom = opprettSykmeldingAktivitet.fom,
                    tom = opprettSykmeldingAktivitet.tom,
                )
        }
    )
}
