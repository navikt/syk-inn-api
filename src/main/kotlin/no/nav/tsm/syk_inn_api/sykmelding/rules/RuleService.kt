package no.nav.tsm.syk_inn_api.sykmelding.rules

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.diagnosekoder.Diagnosekoder
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
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprGodkjenning
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingPayload
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.stereotype.Service

@Service
class RuleService() {
    private val logger = logger()

    fun validateRules(
        payload: OpprettSykmeldingPayload,
        sykmeldingId: String,
        sykmelder: HprSykmelder,
        sykmelderSuspendert: Boolean,
        foedselsdato: LocalDate
    ): RegulaResult {
        return try {
            executeRegulaRules(
                    ruleExecutionPayload =
                        createRegulaPayload(
                            payload = payload,
                            sykmeldingId = sykmeldingId,
                            sykmelder = sykmelder,
                            sykmelderSuspendert = sykmelderSuspendert,
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
        }
    }

    private fun createRegulaPayload(
        payload: OpprettSykmeldingPayload,
        sykmeldingId: String,
        sykmelder: HprSykmelder,
        sykmelderSuspendert: Boolean,
        foedselsdato: LocalDate
    ): RegulaPayload {
        return RegulaPayload(
            sykmeldingId = sykmeldingId,
            hoveddiagnose =
                Diagnose(
                    kode = payload.values.hoveddiagnose.code,
                    system =
                        when (payload.values.hoveddiagnose.system) {
                            DiagnoseSystem.ICPC2 -> Diagnosekoder.ICPC2_CODE
                            DiagnoseSystem.ICD10 -> Diagnosekoder.ICD10_CODE
                        },
                ),
            bidiagnoser = mapToRegulaBidiagnoser(payload.values.bidiagnoser),
            annenFravarsArsak = null,
            aktivitet = mapToSykmeldingAktivitet(payload.values.aktivitet.first()),
            utdypendeOpplysninger = emptyMap(),
            // TODO her bør vi kanskje slå opp tidligere sykmeldinger? sende inn frå kallande
            // service.
            tidligereSykmeldinger = emptyList(),
            kontaktPasientBegrunnelseIkkeKontakt = null,
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
                    suspendert = sykmelderSuspendert,
                    godkjenninger = sykmelder.godkjenninger.map { it.toSykmelderGodkjenning() },
                    legekontorOrgnr = payload.meta.legekontorOrgnr,
                    fnr = sykmelder.fnr,
                ), // TODO bør vi også forholde oss til RegulaBehandler.FinnesIkke?
            avsender =
                RegulaAvsender.Finnes(
                    fnr = sykmelder.fnr,
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
                        DiagnoseSystem.ICPC2 -> Diagnosekoder.ICPC2_CODE
                        DiagnoseSystem.ICD10 -> Diagnosekoder.ICD10_CODE
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

    fun mapToSykmeldingAktivitet(
        opprettSykmeldingAktivitet: OpprettSykmeldingAktivitet
    ): List<no.nav.tsm.regulus.regula.payload.Aktivitet> {
        return listOf(
            when (opprettSykmeldingAktivitet) {
                is OpprettSykmeldingAktivitet.IkkeMulig ->
                    no.nav.tsm.regulus.regula.payload.Aktivitet.IkkeMulig(
                        fom = LocalDate.parse(opprettSykmeldingAktivitet.fom),
                        tom = LocalDate.parse(opprettSykmeldingAktivitet.tom),
                    )
                is OpprettSykmeldingAktivitet.Gradert ->
                    no.nav.tsm.regulus.regula.payload.Aktivitet.Gradert(
                        fom = LocalDate.parse(opprettSykmeldingAktivitet.fom),
                        tom = LocalDate.parse(opprettSykmeldingAktivitet.tom),
                        grad = opprettSykmeldingAktivitet.grad,
                    )
                is OpprettSykmeldingAktivitet.Avventende ->
                    no.nav.tsm.regulus.regula.payload.Aktivitet.Avventende(
                        avventendeInnspillTilArbeidsgiver =
                            opprettSykmeldingAktivitet.innspillTilArbeidsgiver,
                        fom = LocalDate.parse(opprettSykmeldingAktivitet.fom),
                        tom = LocalDate.parse(opprettSykmeldingAktivitet.tom),
                    )
                is OpprettSykmeldingAktivitet.Behandlingsdager ->
                    no.nav.tsm.regulus.regula.payload.Aktivitet.Behandlingsdager(
                        behandlingsdager = opprettSykmeldingAktivitet.antallBehandlingsdager,
                        fom = LocalDate.parse(opprettSykmeldingAktivitet.fom),
                        tom = LocalDate.parse(opprettSykmeldingAktivitet.tom),
                    )
                is OpprettSykmeldingAktivitet.Reisetilskudd ->
                    no.nav.tsm.regulus.regula.payload.Aktivitet.Reisetilskudd(
                        fom = LocalDate.parse(opprettSykmeldingAktivitet.fom),
                        tom = LocalDate.parse(opprettSykmeldingAktivitet.tom),
                    )
            },
        )
    }
}
