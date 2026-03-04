package modules.behandler.api.payloads

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.util.UUID
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarsgrunn
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType

object OpprettSykmelding {
    data class Payload(val submitId: UUID, val meta: BehandlerMeta, val values: Values)

    data class BehandlerMeta(
        val source: String,
        val pasientIdent: String,
        val sykmelderHpr: String,
        val legekontorOrgnr: String,
        val legekontorTlf: String,
    )

    data class Values(
        val pasientenSkalSkjermes: Boolean,
        val hoveddiagnose: DiagnoseInfo,
        val bidiagnoser: List<DiagnoseInfo>,
        val aktivitet: List<Aktivitet>,
        val svangerskapsrelatert: Boolean,
        val meldinger: Meldinger,
        val yrkesskade: Yrkesskade?,
        val arbeidsgiver: Arbeidsgiver?,
        val tilbakedatering: Tilbakedatering?,
        val utdypendeSporsmal: UtdypendeSporsmal?,
        val annenFravarsgrunn: AnnenFravarsgrunn?,
    )

    data class Meldinger(val tilNav: String?, val tilArbeidsgiver: String?)

    data class Yrkesskade(val yrkesskade: Boolean, val skadedato: LocalDate?)

    data class Arbeidsgiver(val harFlere: Boolean, val arbeidsgivernavn: String)

    data class Tilbakedatering(val startdato: LocalDate, val begrunnelse: String)

    data class UtdypendeSporsmal(
        val hensynPaArbeidsplassen: UtdypendeSporsmalSvar?,
        val medisinskOppsummering: UtdypendeSporsmalSvar?,
        val utfordringerMedArbeid: UtdypendeSporsmalSvar?,
        val sykdomsutvikling: UtdypendeSporsmalSvar?,
        val arbeidsrelaterteUtfordringer: UtdypendeSporsmalSvar?,
        val behandlingOgFremtidigArbeid: UtdypendeSporsmalSvar?,
        val uavklarteForhold: UtdypendeSporsmalSvar?,
        val oppdatertMedisinskStatus: UtdypendeSporsmalSvar?,
        val realistiskMestringArbeid: UtdypendeSporsmalSvar?,
        val forventetHelsetilstandUtvikling: UtdypendeSporsmalSvar?,
        val medisinskeHensyn: UtdypendeSporsmalSvar?,
    )

    data class UtdypendeSporsmalSvar(val sporsmalstekst: String, val svar: String)

    @JsonSubTypes(
        JsonSubTypes.Type(Aktivitet.IkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
        JsonSubTypes.Type(Aktivitet.Gradert::class, name = "GRADERT"),
        JsonSubTypes.Type(Aktivitet.Avventende::class, name = "AVVENTENDE"),
        JsonSubTypes.Type(Aktivitet.Behandlingsdager::class, name = "BEHANDLINGSDAGER"),
        JsonSubTypes.Type(Aktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
    )
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    sealed interface Aktivitet {

        val fom: LocalDate
        val tom: LocalDate

        data class IkkeMulig(
            override val fom: LocalDate,
            override val tom: LocalDate,
            val medisinskArsak: MedisinskArsak,
            val arbeidsrelatertArsak: ArbeidsrelatertArsak,
        ) : Aktivitet

        data class Gradert(
            val grad: Int,
            override val fom: LocalDate,
            override val tom: LocalDate,
            val reisetilskudd: Boolean,
        ) : Aktivitet

        data class Behandlingsdager(
            val antallBehandlingsdager: Int,
            override val fom: LocalDate,
            override val tom: LocalDate,
        ) : Aktivitet

        data class Avventende(
            val innspillTilArbeidsgiver: String,
            override val fom: LocalDate,
            override val tom: LocalDate,
        ) : Aktivitet

        data class Reisetilskudd(override val fom: LocalDate, override val tom: LocalDate) :
            Aktivitet
    }

    data class MedisinskArsak(val isMedisinskArsak: Boolean)

    data class ArbeidsrelatertArsak(
        val isArbeidsrelatertArsak: Boolean,
        val arbeidsrelaterteArsaker: List<ArbeidsrelatertArsakType>,
        val annenArbeidsrelatertArsak: String?,
    )

    data class DiagnoseInfo(val system: SykInnDiagnoseSystem, val code: String)
}
