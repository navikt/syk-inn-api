package no.nav.tsm.modules.sykmeldinger.rules.mappers

import java.time.LocalDateTime
import java.util.UUID
import kotlin.collections.listOf
import no.nav.tsm.core.common.SykInnDiagnoseSystem
import no.nav.tsm.diagnoser.ICD10
import no.nav.tsm.diagnoser.ICPC2
import no.nav.tsm.diagnoser.ICPC2B
import no.nav.tsm.modules.sykmeldinger.domain.SykInnAktivitet
import no.nav.tsm.modules.sykmeldinger.domain.SykInnDiagnoseInfo
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.pdl.Identgruppe
import no.nav.tsm.modules.sykmeldinger.pdl.PdlPerson
import no.nav.tsm.modules.sykmeldinger.sykmelder.Sykmelder
import no.nav.tsm.regulus.regula.RegulaAvsender
import no.nav.tsm.regulus.regula.RegulaBehandler
import no.nav.tsm.regulus.regula.RegulaMeta
import no.nav.tsm.regulus.regula.RegulaPasient
import no.nav.tsm.regulus.regula.RegulaPayload
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.regulus.regula.payload.Aktivitet
import no.nav.tsm.regulus.regula.payload.AnnenFravarsArsak
import no.nav.tsm.regulus.regula.payload.Diagnose
import no.nav.tsm.regulus.regula.payload.RelevanteMerknader
import no.nav.tsm.regulus.regula.payload.TidligereSykmelding
import no.nav.tsm.regulus.regula.payload.TidligereSykmeldingAktivitet
import no.nav.tsm.regulus.regula.payload.TidligereSykmeldingMeta
import no.nav.tsm.sykmelding.input.core.model.RuleType

fun Sykmelder.mapSykmelderToRegulaBehandler(legekontorOrgnummer: String): RegulaBehandler =
    when (this) {
        is Sykmelder.FinnesIkke ->
            RegulaBehandler.FinnesIkke(
                // TODO: Fix this quirk in regulus-regula
                fnr = ""
            )

        is Sykmelder.MedSuspensjon ->
            RegulaBehandler.Finnes(
                suspendert = this.suspendert,
                godkjenninger = this.godkjenninger,
                legekontorOrgnr = legekontorOrgnummer,
                fnr = this.ident,
            )
    }

fun PdlPerson.mapPdlPersonToRegulaPasient(): RegulaPasient? {
    val folkeregisterIdent =
        this.identer.firstOrNull { it.gruppe == Identgruppe.FOLKEREGISTERIDENT }
    if (folkeregisterIdent == null) return null
    if (this.foedselsdato == null) return null

    return RegulaPasient(ident = folkeregisterIdent.ident, fodselsdato = this.foedselsdato)
}

fun mapUnruledSykInnSykmeldingToRegulaPayload(
    behandletTidspunkt: LocalDateTime,
    sykmelding: UnverifiedSykInnSykmelding,
    otherSykmeldinger: List<VerifiedSykInnSykmelding>,
    behandler: RegulaBehandler,
    pasient: RegulaPasient,
): RegulaPayload {
    val avsender =
        if (behandler is RegulaBehandler.Finnes) RegulaAvsender.Finnes(fnr = behandler.fnr)
        else RegulaAvsender.IngenAvsender

    return RegulaPayload(
        meta = RegulaMeta.Meta(sendtTidspunkt = LocalDateTime.now()),
        pasient = pasient,
        behandler = behandler,
        avsender = avsender,
        hoveddiagnose = sykmelding.values.hoveddiagnose?.toRegulaDiagnose(),
        bidiagnoser = sykmelding.values.bidiagnoser.map { it.toRegulaDiagnose() },
        aktivitet = sykmelding.values.aktivitet.map { it.toRegulaAktivitet() },
        annenFravarsArsak =
            sykmelding.values.annenFravarsgrunn?.let {
                AnnenFravarsArsak(grunn = listOf(it.name), beskrivelse = null)
            },
        // TODO: Provide
        tidligereSykmeldinger = otherSykmeldinger.map { it.toTidligereSykmelding() },
        utdypendeOpplysninger = emptyMap(),
        kontaktPasientBegrunnelseIkkeKontakt = sykmelding.values.tilbakedatering?.begrunnelse,
        behandletTidspunkt = behandletTidspunkt,
    )
}

private fun SykInnDiagnoseInfo.toRegulaDiagnose(): Diagnose {
    return Diagnose(
        kode = code,
        system =
            when (system) {
                SykInnDiagnoseSystem.ICPC2 -> ICPC2.OID
                SykInnDiagnoseSystem.ICD10 -> ICD10.OID
                SykInnDiagnoseSystem.ICPC2B -> ICPC2B.OID
            },
    )
}

private fun SykInnAktivitet.toRegulaAktivitet(): Aktivitet =
    when (this) {
        is SykInnAktivitet.IkkeMulig -> Aktivitet.IkkeMulig(fom = fom, tom = tom)
        is SykInnAktivitet.Gradert -> Aktivitet.Gradert(fom = fom, tom = tom, grad = grad)
        is SykInnAktivitet.Avventende ->
            Aktivitet.Avventende(
                fom = fom,
                tom = tom,
                avventendeInnspillTilArbeidsgiver = innspillTilArbeidsgiver,
            )
        is SykInnAktivitet.Behandlingsdager ->
            Aktivitet.Behandlingsdager(
                fom = fom,
                tom = tom,
                behandlingsdager = antallBehandlingsdager,
            )
        is SykInnAktivitet.Reisetilskudd -> Aktivitet.Reisetilskudd(fom = fom, tom = tom)
    }

private fun VerifiedSykInnSykmelding.toTidligereSykmelding(): TidligereSykmelding {
    return TidligereSykmelding(
        sykmeldingId = sykmeldingId.toString(),
        hoveddiagnose = values.hoveddiagnose?.toRegulaDiagnose(),
        aktivitet = values.aktivitet.map { it.toTidligereAktivitet() },
        meta =
            TidligereSykmeldingMeta(
                status =
                    when (result) {
                        is SykInnSykmeldingRuleResult.OK -> RegulaStatus.OK
                        is SykInnSykmeldingRuleResult.Outcome ->
                            when (result.type) {
                                RuleType.OK -> RegulaStatus.OK
                                RuleType.PENDING -> RegulaStatus.MANUAL_PROCESSING
                                RuleType.INVALID -> RegulaStatus.INVALID
                            }
                    },
                userAction = "IKKE_RELEVANT",
                merknader =
                    if (
                        result is SykInnSykmeldingRuleResult.Outcome &&
                            result.type == RuleType.PENDING
                    ) {
                        listOf(RelevanteMerknader.UNDER_BEHANDLING)
                    } else emptyList(),
            ),
    )
}

private fun SykInnAktivitet.toTidligereAktivitet(): TidligereSykmeldingAktivitet =
    when (this) {
        is SykInnAktivitet.Avventende ->
            TidligereSykmeldingAktivitet.Avventende(fom = fom, tom = tom)
        is SykInnAktivitet.Behandlingsdager ->
            TidligereSykmeldingAktivitet.Behandlingsdager(fom = fom, tom = tom)
        is SykInnAktivitet.Gradert ->
            TidligereSykmeldingAktivitet.Gradert(fom = fom, tom = tom, grad = grad)
        is SykInnAktivitet.IkkeMulig -> TidligereSykmeldingAktivitet.IkkeMulig(fom = fom, tom = tom)
        is SykInnAktivitet.Reisetilskudd ->
            TidligereSykmeldingAktivitet.Reisetilskudd(fom = fom, tom = tom)
    }
