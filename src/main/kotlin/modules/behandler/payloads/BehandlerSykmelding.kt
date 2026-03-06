package modules.behandler.payloads

import java.time.LocalDate
import java.time.OffsetDateTime
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarsgrunn
import no.nav.tsm.sykmelding.input.core.model.RuleType

/** This sealed interface allows both "Full" and "Redacted" versions of the sykmelding response. */
sealed interface BehandlerSykmelding {
    val sykmeldingId: String
    val meta: BehandlerSykmeldingMeta
    val utfall: BehandlerSykmeldingRuleResult
}

data class BehandlerSykmeldingFull(
    override val sykmeldingId: String,
    override val meta: BehandlerSykmeldingMeta,
    override val utfall: BehandlerSykmeldingRuleResult,
    val values: BehandlerSykmeldingValues,
) : BehandlerSykmelding

data class BehandlerSykmeldingMeta(
    val mottatt: OffsetDateTime,
    val pasientIdent: String,
    val sykmelder: BehandlerSykmeldingSykmelder,
    val legekontorOrgnr: String?,
    val legekontorTlf: String?,
)

data class BehandlerSykmeldingValues(
    val hoveddiagnose: BehandlerSykmeldingDiagnoseInfo?,
    val bidiagnoser: List<BehandlerSykmeldingDiagnoseInfo>?,
    val aktivitet: List<BehandlerSykmeldingAktivitet>,
    val svangerskapsrelatert: Boolean,
    val pasientenSkalSkjermes: Boolean,
    val meldinger: BehandlerSykmeldingMeldinger,
    val yrkesskade: BehandlerSykmeldingYrkesskade?,
    val arbeidsgiver: BehandlerSykmeldingArbeidsgiver?,
    val tilbakedatering: BehandlerSykmeldingTilbakedatering?,
    val utdypendeSporsmal: BehandlerSykmeldingUtdypendeSporsmal?,
    val utdypendeSporsmalSvar: BehandlerSykmeldingUtdypendeSporsmalSvar?,
    val annenFravarsgrunn: AnnenFravarsgrunn?,
)

sealed class BehandlerSykmeldingAktivitet(open val fom: LocalDate, open val tom: LocalDate) {
    data class IkkeMulig(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val medisinskArsak: BehandlerSykmeldingMedisinskArsak,
        val arbeidsrelatertArsak: BehandlerSykmeldingArbeidsrelatertArsak,
    ) : BehandlerSykmeldingAktivitet(fom, tom)

    data class Gradert(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val grad: Int,
        val reisetilskudd: Boolean,
    ) : BehandlerSykmeldingAktivitet(fom, tom)

    data class Behandlingsdager(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val antallBehandlingsdager: Int,
    ) : BehandlerSykmeldingAktivitet(fom, tom)

    data class Avventende(
        val innspillTilArbeidsgiver: String,
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : BehandlerSykmeldingAktivitet(fom, tom)

    data class Reisetilskudd(override val fom: LocalDate, override val tom: LocalDate) :
        BehandlerSykmeldingAktivitet(fom, tom)
}

data class BehandlerSykmeldingMedisinskArsak(val isMedisinskArsak: Boolean)

data class BehandlerSykmeldingArbeidsrelatertArsak(
    val isArbeidsrelatertArsak: Boolean,
    val arbeidsrelaterteArsaker: List<SykInnArbeidsrelatertArsakType>,
    val annenArbeidsrelatertArsak: String?,
)

enum class SykInnArbeidsrelatertArsakType {
    TILRETTELEGGING_IKKE_MULIG,
    ANNET,
}

data class BehandlerSykmeldingDiagnoseInfo(
    val system: SykInnDiagnoseSystem,
    val code: String,
    val text: String,
)

data class BehandlerSykmeldingRuleResult(val result: RuleType, val melding: String?)

data class BehandlerSykmeldingTilbakedatering(val startdato: LocalDate, val begrunnelse: String)

data class BehandlerSykmeldingArbeidsgiver(val harFlere: Boolean, val arbeidsgivernavn: String)

data class BehandlerSykmeldingYrkesskade(val yrkesskade: Boolean, val skadedato: LocalDate?)

data class BehandlerSykmeldingMeldinger(val tilNav: String?, val tilArbeidsgiver: String?)

data class BehandlerSykmeldingSykmelder(
    val hprNummer: String,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
)

data class BehandlerSykmeldingUtdypendeSporsmal(
    val utfordringerMedArbeid: String?,
    val medisinskOppsummering: String?,
    val hensynPaArbeidsplassen: String?,
)

data class BehandlerSykmeldingUtdypendeSporsmalSvar(
    val utfordringerMedArbeid: BehandlerSykmeldingSporsmalSvar?,
    val medisinskOppsummering: BehandlerSykmeldingSporsmalSvar?,
    val hensynPaArbeidsplassen: BehandlerSykmeldingSporsmalSvar?,
    val sykdomsutvikling: BehandlerSykmeldingSporsmalSvar?,
    val arbeidsrelaterteUtfordringer: BehandlerSykmeldingSporsmalSvar?,
    val behandlingOgFremtidigArbeid: BehandlerSykmeldingSporsmalSvar?,
    val uavklarteForhold: BehandlerSykmeldingSporsmalSvar?,
    val oppdatertMedisinskStatus: BehandlerSykmeldingSporsmalSvar?,
    val realistiskMestringArbeid: BehandlerSykmeldingSporsmalSvar?,
    val forventetHelsetilstandUtvikling: BehandlerSykmeldingSporsmalSvar?,
    val medisinskeHensyn: BehandlerSykmeldingSporsmalSvar?,
)

data class BehandlerSykmeldingSporsmalSvar(val sporsmalstekst: String?, val svar: String)
