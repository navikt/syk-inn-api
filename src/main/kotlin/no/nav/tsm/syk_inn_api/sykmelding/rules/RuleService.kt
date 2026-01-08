package no.nav.tsm.syk_inn_api.sykmelding.rules

import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.tsm.diagnoser.ICD10
import no.nav.tsm.diagnoser.ICPC2
import no.nav.tsm.diagnoser.ICPC2B
import no.nav.tsm.regulus.regula.RegulaAvsender
import no.nav.tsm.regulus.regula.RegulaBehandler
import no.nav.tsm.regulus.regula.RegulaMeta
import no.nav.tsm.regulus.regula.RegulaPasient
import no.nav.tsm.regulus.regula.RegulaPayload
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.executeRegulaRules
import no.nav.tsm.regulus.regula.executor.ExecutionMode
import no.nav.tsm.regulus.regula.payload.BehandlerGodkjenning
import no.nav.tsm.regulus.regula.payload.BehandlerKode
import no.nav.tsm.regulus.regula.payload.BehandlerPeriode
import no.nav.tsm.regulus.regula.payload.BehandlerTilleggskompetanse
import no.nav.tsm.regulus.regula.payload.Diagnose
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprGodkjenning
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingPayload
import no.nav.tsm.syk_inn_api.utils.failSpan
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.stereotype.Service

@Service
class RuleService() {
    private val logger = logger()

    @WithSpan
    fun validateRules(
        payload: OpprettSykmeldingPayload,
        sykmeldingId: String,
        sykmelder: Sykmelder.MedSuspensjon,
        foedselsdato: LocalDate
    ): RegulaResult {
        return try {
            executeRegulaRules(
                    ruleExecutionPayload =
                        createRegulaPayload(
                            payload = payload,
                            sykmeldingId = sykmeldingId,
                            sykmelder = sykmelder,
                            foedselsdato = foedselsdato,
                        ),
                    mode = ExecutionMode.NORMAL,
                )
                .also {
                    logger.info(
                        "Sykmelding med id=$sykmeldingId er validering ${it.status.name} mot regler",
                    )
                }
        } catch (e: Exception) {
            logger.error("Error while executing Regula rules", e)
            throw RuntimeException(
                    "Error while executing Regula rules for sykmeldingId=$sykmeldingId",
                )
                .failSpan()
        }
    }

    private fun createRegulaPayload(
        payload: OpprettSykmeldingPayload,
        sykmeldingId: String,
        sykmelder: Sykmelder.MedSuspensjon,
        foedselsdato: LocalDate
    ): RegulaPayload {
        return RegulaPayload(
            sykmeldingId = sykmeldingId,
            hoveddiagnose =
                payload.values.hoveddiagnose.let { diagnose ->
                    Diagnose(
                        kode = diagnose.code,
                        system =
                            when (diagnose.system) {
                                DiagnoseSystem.ICPC2 -> ICPC2.OID
                                DiagnoseSystem.ICD10 -> ICD10.OID
                                DiagnoseSystem.ICPC2B -> ICPC2B.OID
                                DiagnoseSystem.PHBU -> "2.16.578.1.12.4.1.1.7112"
                                DiagnoseSystem.UGYLDIG -> "UGYLDIG"
                            },
                    )
                },
            bidiagnoser = mapToRegulaBidiagnoser(payload.values.bidiagnoser),
            annenFravarsArsak = null,
            aktivitet = mapToSykmeldingAktivitet(payload.values.aktivitet.first()),
            utdypendeOpplysninger = emptyMap(),
            // TODO her bør vi kanskje slå opp tidligere sykmeldinger? sende inn frå kallande
            // service.
            tidligereSykmeldinger = emptyList(),
            kontaktPasientBegrunnelseIkkeKontakt = payload.values.tilbakedatering?.begrunnelse,
            pasient =
                RegulaPasient(
                    ident = payload.meta.pasientIdent,
                    fodselsdato = foedselsdato,
                ),
            meta =
                RegulaMeta.Meta(
                    sendtTidspunkt = LocalDateTime.now(),
                ),
            behandler =
                RegulaBehandler.Finnes(
                    suspendert = sykmelder.suspendert,
                    godkjenninger = sykmelder.godkjenninger.map { it.toSykmelderGodkjenning() },
                    legekontorOrgnr = payload.meta.legekontorOrgnr,
                    fnr = sykmelder.ident,
                ), // TODO bør vi også forholde oss til RegulaBehandler.FinnesIkke?
            avsender =
                RegulaAvsender.Finnes(
                    fnr = sykmelder.ident,
                ), // TODO Bør vi også forholde oss til Avsender.FinnesIkke ?
            behandletTidspunkt = LocalDateTime.now(),
        )
    }

    private fun mapToRegulaBidiagnoser(
        bidiagnoser: List<OpprettSykmeldingDiagnoseInfo>?
    ): List<Diagnose>? {
        if (bidiagnoser.isNullOrEmpty()) {
            return null
        }

        return bidiagnoser.map { diagnose ->
            Diagnose(
                kode = diagnose.code,
                system =
                    when (diagnose.system) {
                        DiagnoseSystem.ICPC2 -> ICPC2.OID
                        DiagnoseSystem.ICD10 -> ICD10.OID
                        DiagnoseSystem.ICPC2B -> ICPC2B.OID
                        DiagnoseSystem.PHBU -> "2.16.578.1.12.4.1.1.7112"
                        DiagnoseSystem.UGYLDIG -> "UGYLDIG"
                    },
            )
        }
    }

    private fun HprGodkjenning.toSykmelderGodkjenning() =
        BehandlerGodkjenning(
            helsepersonellkategori =
                helsepersonellkategori?.let {
                    BehandlerKode(
                        oid = it.oid,
                        aktiv = it.aktiv,
                        verdi = it.verdi,
                    )
                },
            tillegskompetanse =
                tillegskompetanse?.map { tillegskompetanse ->
                    BehandlerTilleggskompetanse(
                        avsluttetStatus =
                            tillegskompetanse.avsluttetStatus?.let {
                                BehandlerKode(
                                    oid = it.oid,
                                    aktiv = it.aktiv,
                                    verdi = it.verdi,
                                )
                            },
                        gyldig =
                            tillegskompetanse.gyldig?.let {
                                BehandlerPeriode(fra = it.fra, til = it.til)
                            },
                        type =
                            tillegskompetanse.type?.let {
                                BehandlerKode(
                                    oid = it.oid,
                                    aktiv = it.aktiv,
                                    verdi = it.verdi,
                                )
                            },
                    )
                },
            autorisasjon =
                autorisasjon?.let {
                    BehandlerKode(
                        oid = it.oid,
                        aktiv = it.aktiv,
                        verdi = it.verdi,
                    )
                },
        )

    private fun mapToSykmeldingAktivitet(
        opprettSykmeldingAktivitet: OpprettSykmeldingAktivitet
    ): List<no.nav.tsm.regulus.regula.payload.Aktivitet> {
        return listOf(
            when (opprettSykmeldingAktivitet) {
                is OpprettSykmeldingAktivitet.IkkeMulig ->
                    no.nav.tsm.regulus.regula.payload.Aktivitet.IkkeMulig(
                        fom = opprettSykmeldingAktivitet.fom,
                        tom = opprettSykmeldingAktivitet.tom,
                    )
                is OpprettSykmeldingAktivitet.Gradert ->
                    no.nav.tsm.regulus.regula.payload.Aktivitet.Gradert(
                        fom = opprettSykmeldingAktivitet.fom,
                        tom = opprettSykmeldingAktivitet.tom,
                        grad = opprettSykmeldingAktivitet.grad,
                    )
                is OpprettSykmeldingAktivitet.Avventende ->
                    no.nav.tsm.regulus.regula.payload.Aktivitet.Avventende(
                        avventendeInnspillTilArbeidsgiver =
                            opprettSykmeldingAktivitet.innspillTilArbeidsgiver,
                        fom = opprettSykmeldingAktivitet.fom,
                        tom = opprettSykmeldingAktivitet.tom,
                    )
                is OpprettSykmeldingAktivitet.Behandlingsdager ->
                    no.nav.tsm.regulus.regula.payload.Aktivitet.Behandlingsdager(
                        behandlingsdager = opprettSykmeldingAktivitet.antallBehandlingsdager,
                        fom = opprettSykmeldingAktivitet.fom,
                        tom = opprettSykmeldingAktivitet.tom,
                    )
                is OpprettSykmeldingAktivitet.Reisetilskudd ->
                    no.nav.tsm.regulus.regula.payload.Aktivitet.Reisetilskudd(
                        fom = opprettSykmeldingAktivitet.fom,
                        tom = opprettSykmeldingAktivitet.tom,
                    )
            },
        )
    }
}
