package no.nav.tsm.modules.behandler.payloads

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarsgrunn
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.RuleType

/** This sealed interface allows both "Full" and "Redacted" versions of the sykmelding response. */
sealed interface BehandlerSykmelding {
    val sykmeldingId: UUID
    val meta: BehandlerSykmeldingMeta
}

data class BehandlerSykmeldingFull(
    override val sykmeldingId: UUID,
    override val meta: BehandlerSykmeldingMeta,
    val utfall: BehandlerSykmeldingRuleResult,
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
    val meldinger: BehandlerSykmeldingMeldinger?,
    val yrkesskade: BehandlerSykmeldingYrkesskade?,
    val arbeidsgiver: BehandlerSykmeldingArbeidsgiver?,
    val tilbakedatering: BehandlerSykmeldingTilbakedatering?,
    val utdypendeSporsmal: BehandlerSykmeldingUtdypendeSporsmal?,
    val annenFravarsgrunn: AnnenFravarsgrunn?,
)

sealed class BehandlerSykmeldingAktivitet(
    val type: BehandlerSykmeldingType,
    open val fom: LocalDate,
    open val tom: LocalDate,
) {
    enum class BehandlerSykmeldingType {
        AKTIVITET_IKKE_MULIG,
        GRADERT,
        AVVENTENDE,
        BEHANDLINGSDAGER,
        REISETILSKUDD,
    }

    data class IkkeMulig(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val medisinskArsak: BehandlerSykmeldingMedisinskArsak,
        val arbeidsrelatertArsak: BehandlerSykmeldingArbeidsrelatertArsak,
    ) : BehandlerSykmeldingAktivitet(BehandlerSykmeldingType.AKTIVITET_IKKE_MULIG, fom, tom)

    data class Gradert(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val grad: Int,
        val reisetilskudd: Boolean,
    ) : BehandlerSykmeldingAktivitet(BehandlerSykmeldingType.GRADERT, fom, tom)

    data class Behandlingsdager(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val antallBehandlingsdager: Int,
    ) : BehandlerSykmeldingAktivitet(BehandlerSykmeldingType.BEHANDLINGSDAGER, fom, tom)

    data class Avventende(
        val innspillTilArbeidsgiver: String,
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : BehandlerSykmeldingAktivitet(BehandlerSykmeldingType.AVVENTENDE, fom, tom)

    data class Reisetilskudd(override val fom: LocalDate, override val tom: LocalDate) :
        BehandlerSykmeldingAktivitet(BehandlerSykmeldingType.REISETILSKUDD, fom, tom)
}

data class BehandlerSykmeldingMedisinskArsak(val isMedisinskArsak: Boolean)

data class BehandlerSykmeldingArbeidsrelatertArsak(
    val isArbeidsrelatertArsak: Boolean,
    val arbeidsrelaterteArsaker: List<ArbeidsrelatertArsakType>,
    val annenArbeidsrelatertArsak: String?,
)

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
    // TODO: Fikse i syk-inn schema
    val hpr: String,
    // TODO: Fikse i syk-inn schema
    val navn: String,
)

data class BehandlerSykmeldingUtdypendeSporsmal(
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

data class BehandlerSykmeldingSporsmalSvar(val sporsmalstekst: String, val svar: String)
