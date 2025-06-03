package no.nav.tsm.syk_inn_api.sykmelding.rules

import java.time.LocalDate
import java.time.LocalDateTime
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
import no.nav.tsm.syk_inn_api.exception.RuleHitException
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprGodkjenning
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.SykmeldingPayload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RuleService() {
    private val logger = LoggerFactory.getLogger(RuleService::class.java)

    fun validateRules(
        payload: SykmeldingPayload,
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
        } catch (e: Exception) {
            logger.error("Error while executing Regula rules", e)
            throw RuleHitException(
                "Error while executing Regula rules for sykmeldingId=$sykmeldingId",
            )
        }
    }

    private fun createRegulaPayload(
        payload: SykmeldingPayload,
        sykmeldingId: String,
        sykmelder: HprSykmelder,
        sykmelderSuspendert: Boolean,
        foedselsdato: LocalDate
    ): RegulaPayload {
        return RegulaPayload(
            sykmeldingId = sykmeldingId,
            hoveddiagnose =
                Diagnose(
                    kode = payload.sykmelding.hoveddiagnose.code,
                    system = payload.sykmelding.hoveddiagnose.system.oid,
                ),
            bidiagnoser = null,
            annenFravarsArsak = null,
            aktivitet =
                listOf(mapToSykmeldingAktivitet(payload.sykmelding.opprettSykmeldingAktivitet)),
            utdypendeOpplysninger = emptyMap(),
            tidligereSykmeldinger = emptyList(),
            kontaktPasientBegrunnelseIkkeKontakt = null,
            pasient =
                RegulaPasient(
                    ident = payload.pasientFnr,
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
                    legekontorOrgnr = payload.legekontorOrgnr,
                    fnr = sykmelder.fnr,
                ), // TODO bør vi også forholde oss til RegulaBehandler.FinnesIkke?
            avsender =
                RegulaAvsender.Finnes(
                    fnr = sykmelder.fnr,
                ), // TODO Bør vi også forholde oss til Avsender.FinnesIkke ?
            behandletTidspunkt = LocalDateTime.now(),
        )
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
    ): no.nav.tsm.regulus.regula.payload.Aktivitet {
        return when (opprettSykmeldingAktivitet) {
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
        }
    }
}
