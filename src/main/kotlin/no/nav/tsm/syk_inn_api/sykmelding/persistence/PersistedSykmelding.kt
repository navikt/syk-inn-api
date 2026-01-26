package no.nav.tsm.syk_inn_api.sykmelding.persistence

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.person.Navn
import no.nav.tsm.syk_inn_api.sykmelding.response.SykInnArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarsgrunn

data class PersistedSykmelding(
    val sykmeldingId: String,
    val pasient: PersistedSykmeldingPasient,
    val sykmelder: PersistedSykmeldingSykmelder,
    val hoveddiagnose: PersistedSykmeldingDiagnoseInfo?,
    val bidiagnoser: List<PersistedSykmeldingDiagnoseInfo>,
    val aktivitet: List<PersistedSykmeldingAktivitet>,
    val svangerskapsrelatert: Boolean,
    val pasientenSkalSkjermes: Boolean,
    val meldinger: PersistedSykmeldingMeldinger,
    val yrkesskade: PersistedSykmeldingYrkesskade?,
    val arbeidsgiver: PersistedSykmeldingArbeidsgiver?,
    val tilbakedatering: PersistedSykmeldingTilbakedatering?,
    val utdypendeSporsmal: PersistedSykmeldingUtdypendeSporsmal?,
    val utdypendeSporsmalQuestionText: PersistedSykmeldingUtdypendeSporsmalQuestionText?,
    val annenFravarsgrunn: AnnenFravarsgrunn?,
    val regelResultat: PersistedSykmeldingRuleResult,
)

enum class PersistedRuleType {
    OK,
    PENDING,
    INVALID,
}

enum class PersistedValidationType {
    AUTOMATIC,
    MANUAL,
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PersistedOKRule::class, name = "OK"),
    JsonSubTypes.Type(value = PersistedInvalidRule::class, name = "INVALID"),
    JsonSubTypes.Type(value = PersistedPendingRule::class, name = "PENDING"),
)
sealed interface PersistedRule {
    val type: PersistedRuleType
    val name: String
    val validationType: PersistedValidationType
    val timestamp: OffsetDateTime
}

data class PersistedReason(val sykmeldt: String, val sykmelder: String)

data class PersistedInvalidRule(
    override val name: String,
    override val validationType: PersistedValidationType,
    override val timestamp: OffsetDateTime,
    val reason: PersistedReason,
) : PersistedRule {
    override val type = PersistedRuleType.INVALID
}

data class PersistedPendingRule(
    override val name: String,
    override val timestamp: OffsetDateTime,
    override val validationType: PersistedValidationType,
    val reason: PersistedReason,
) : PersistedRule {
    override val type = PersistedRuleType.PENDING
}

data class PersistedOKRule(
    override val name: String,
    override val timestamp: OffsetDateTime,
    override val validationType: PersistedValidationType,
) : PersistedRule {
    override val type = PersistedRuleType.OK
}

data class PersistedValidationResult(
    val status: PersistedRuleType,
    val timestamp: OffsetDateTime,
    val rules: List<PersistedRule>,
)

data class PersistedSykmeldingRuleResult(
    val result: PersistedRuleType,
    val meldingTilSender: String?,
)

data class PersistedSykmeldingSykmelder(
    val godkjenninger: List<PersistedSykmeldingHprGodkjenning> = emptyList(),
    val ident: String,
    val hprNummer: String,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
)

data class PersistedSykmeldingPasient(
    val navn: Navn,
    val ident: String,
    val fodselsdato: LocalDate,
)

data class PersistedSykmeldingMeldinger(
    val tilNav: String?,
    val tilArbeidsgiver: String?,
)

data class PersistedSykmeldingYrkesskade(
    val yrkesskade: Boolean,
    val skadedato: LocalDate?,
)

data class PersistedSykmeldingArbeidsgiver(
    val harFlere: Boolean,
    val arbeidsgivernavn: String,
)

data class PersistedSykmeldingTilbakedatering(
    val startdato: LocalDate,
    val begrunnelse: String,
)

data class PersistedSykmeldingUtdypendeSporsmal(
    val hensynPaArbeidsplassen: String?,
    val medisinskOppsummering: String?,
    val utfordringerMedArbeid: String?,
)

data class PersistedSykmeldingUtdypendeSporsmalQuestionText(
    val hensynPaArbeidsplassen: String?,
    val medisinskOppsummering: String?,
    val utfordringerMedArbeid: String?,
)

@JsonSubTypes(
    JsonSubTypes.Type(PersistedSykmeldingAktivitet.IkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(PersistedSykmeldingAktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(PersistedSykmeldingAktivitet.Avventende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(
        PersistedSykmeldingAktivitet.Behandlingsdager::class,
        name = "BEHANDLINGSDAGER"
    ),
    JsonSubTypes.Type(PersistedSykmeldingAktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface PersistedSykmeldingAktivitet {
    val fom: LocalDate
    val tom: LocalDate

    data class IkkeMulig(
        val medisinskArsak: PersistedSykmeldingMedisinskArsak,
        val arbeidsrelatertArsak: PersistedSykmeldingArbeidsrelatertArsak,
        override val fom: LocalDate,
        override val tom: LocalDate
    ) : PersistedSykmeldingAktivitet

    data class Gradert(
        val grad: Int,
        val reisetilskudd: Boolean,
        override val fom: LocalDate,
        override val tom: LocalDate
    ) : PersistedSykmeldingAktivitet

    data class Behandlingsdager(
        val antallBehandlingsdager: Int,
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : PersistedSykmeldingAktivitet

    data class Avventende(
        val innspillTilArbeidsgiver: String,
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : PersistedSykmeldingAktivitet

    data class Reisetilskudd(override val fom: LocalDate, override val tom: LocalDate) :
        PersistedSykmeldingAktivitet
}

data class PersistedSykmeldingMedisinskArsak(val isMedisinskArsak: Boolean)

data class PersistedSykmeldingArbeidsrelatertArsak(
    val isArbeidsrelatertArsak: Boolean,
    val arbeidsrelaterteArsaker: List<SykInnArbeidsrelatertArsakType>,
    val annenArbeidsrelatertArsak: String?,
)

data class PersistedSykmeldingDiagnoseInfo(
    val system: DiagnoseSystem,
    val code: String,
    val text: String,
)

data class PersistedSykmeldingHprGodkjenning(
    val helsepersonellkategori: PersistedSykmeldingHprKode? = null,
    val autorisasjon: PersistedSykmeldingHprKode? = null,
    val tillegskompetanse: List<PersistedSykmeldingHprTilleggskompetanse>? = null,
)

data class PersistedSykmeldingHprTilleggskompetanse(
    val avsluttetStatus: PersistedSykmeldingHprKode?,
    @param:JsonAlias("etag", "eTag") val eTag: String?,
    val gyldig: PersistedSykmeldingHprGyldighetsPeriode?,
    val id: Int?,
    val type: PersistedSykmeldingHprKode?
)

data class PersistedSykmeldingHprGyldighetsPeriode(
    val fra: LocalDateTime?,
    val til: LocalDateTime?
)

data class PersistedSykmeldingHprKode(
    val aktiv: Boolean,
    val oid: Int,
    val verdi: String?,
)
