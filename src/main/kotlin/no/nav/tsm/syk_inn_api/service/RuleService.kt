package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.regulus.regula.RegulaAvsender
import no.nav.tsm.regulus.regula.RegulaBehandler
import no.nav.tsm.regulus.regula.RegulaMeta
import no.nav.tsm.regulus.regula.RegulaPasient
import no.nav.tsm.regulus.regula.RegulaPayload
import no.nav.tsm.regulus.regula.payload.BehandlerGodkjenning
import no.nav.tsm.regulus.regula.payload.BehandlerKode
import no.nav.tsm.regulus.regula.payload.BehandlerPeriode
import no.nav.tsm.regulus.regula.payload.BehandlerTilleggskompetanse
import no.nav.tsm.regulus.regula.payload.Diagnose
import no.nav.tsm.syk_inn_api.model.Aktivitet
import no.nav.tsm.syk_inn_api.model.Sykmelder
import no.nav.tsm.syk_inn_api.model.Godkjenning
import no.nav.tsm.syk_inn_api.model.SykmeldingPayload
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class RuleService(
    private val pdlService: PdlService,
    private val btsysProxyService: BtsysProxyService,
) {

    fun validateRules(
        payload: SykmeldingPayload,
        sykmeldingId: String,
        sykmelder: Sykmelder
    ): Boolean {

        val regulaPayload = createRegulaPayload(payload, sykmeldingId, sykmelder)
        //todo validation against rules
        return true
    }

    private fun createRegulaPayload(
        payload: SykmeldingPayload,
        sykmeldingId: String,
        sykmelder: Sykmelder
    ): RegulaPayload {
        return RegulaPayload(
            sykmeldingId = sykmeldingId,
            hoveddiagnose = Diagnose(
                kode = payload.sykmelding.hoveddiagnose.code,
                system = payload.sykmelding.hoveddiagnose.system.name,
            ),
            bidiagnoser = null,
            annenFravarsArsak = null,
            aktivitet = listOf(mapToSykmeldingAktivitet(payload.sykmelding.aktivitet)),
            utdypendeOpplysninger = emptyMap(),
            tidligereSykmeldinger = emptyList(),
            kontaktPasientBegrunnelseIkkeKontakt = null,
            pasient = RegulaPasient(
                ident = payload.pasientFnr,
                fodselsdato = pdlService.getFodselsdato(payload.pasientFnr),
            ),
            meta = RegulaMeta.Meta(
                sendtTidspunkt = LocalDateTime.now(),
            ),
            behandler = RegulaBehandler(
                suspendert = btsysProxyService.isSuspended(
                    sykmelderFnr = sykmelder.fnr,
                    signaturDato = LocalDateTime.now().toString(),
                ),
                godkjenninger = sykmelder.godkjenninger.map {
                    it.toBehandlerGodkjenning()
                },
                legekontorOrgnr = "123456789", //TODO where do we find this, maybe its in Practitioner? or another fhir endpoint?
                fnr = sykmelder.fnr,
            ),
            avsender = RegulaAvsender(
                fnr = sykmelder.fnr,
            ),
            behandletTidspunkt = LocalDateTime.now(),
        )
    }


    private fun Godkjenning.toBehandlerGodkjenning() =
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

    fun mapToSykmeldingAktivitet(aktivitet: Aktivitet): no.nav.tsm.regulus.regula.payload.Aktivitet {
        return when (aktivitet) {
            is Aktivitet.IkkeMulig -> no.nav.tsm.regulus.regula.payload.Aktivitet.IkkeMulig(
                fom = LocalDate.parse(aktivitet.fom),
                tom = LocalDate.parse(aktivitet.tom)
            )

            is Aktivitet.Gradert -> no.nav.tsm.regulus.regula.payload.Aktivitet.Gradert(
                fom = LocalDate.parse(aktivitet.fom),
                tom = LocalDate.parse(aktivitet.tom),
                grad = aktivitet.grad
            )

            is Aktivitet.Ugyldig -> no.nav.tsm.regulus.regula.payload.Aktivitet.Ugyldig(
                fom = LocalDate.parse(aktivitet.fom),
                tom = LocalDate.parse(aktivitet.tom)
            )
        }
    }
}


//bidiagnoser = null
//annenFravarsArsak = null,
//utdypendeOpplysninger = null // eller empty map? Burde kanskje støtte null
//kontaktPasientBegrunnelseIkkeKontakt = null,
//8:12
//resten er inferred/fetched/provida fra fhir
