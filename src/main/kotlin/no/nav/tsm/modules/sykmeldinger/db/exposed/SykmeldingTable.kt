package no.nav.tsm.modules.sykmeldinger.db.exposed

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.OptBoolean
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.RuleType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import org.jetbrains.exposed.v1.json.jsonb

data class SykmeldingJsonbRuleResult(val type: RuleType, val message: String?, val rule: String?)

data class SykmeldingJsonbDiagnose(val system: String, val text: String, val code: String)

data class SykmeldingJsonbMeldinger(val tilNav: String?, val tilArbeidsgiver: String?)

data class SykmeldingJsonbYrkesskade(val yrkesskade: Boolean, val skadedato: LocalDate?)

data class SykmeldingJsonbArbeidsgiver(val harFlere: Boolean, val arbeidsgivernavn: String)

data class SykmeldingJsonbTilbakedatering(val startdato: LocalDate, val begrunnelse: String)

@JsonSubTypes(
    JsonSubTypes.Type(SykmeldingJsonbAktivitet.IkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(SykmeldingJsonbAktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(SykmeldingJsonbAktivitet.Avventende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(SykmeldingJsonbAktivitet.Behandlingsdager::class, name = "BEHANDLINGSDAGER"),
    JsonSubTypes.Type(SykmeldingJsonbAktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    requireTypeIdForSubtypes = OptBoolean.TRUE,
)
sealed interface SykmeldingJsonbAktivitet {
    val fom: LocalDate
    val tom: LocalDate

    data class IkkeMulig(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val medisinskArsak: SykmeldingJsonbMedisinskArsak,
        val arbeidsrelatertArsak: SykmeldingJsonbArbeidsrelatertArsak,
    ) : SykmeldingJsonbAktivitet

    data class Gradert(
        val grad: Int,
        override val fom: LocalDate,
        override val tom: LocalDate,
        val reisetilskudd: Boolean,
    ) : SykmeldingJsonbAktivitet

    data class Behandlingsdager(
        val antallBehandlingsdager: Int,
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : SykmeldingJsonbAktivitet

    data class Avventende(
        val innspillTilArbeidsgiver: String,
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : SykmeldingJsonbAktivitet

    data class Reisetilskudd(override val fom: LocalDate, override val tom: LocalDate) :
        SykmeldingJsonbAktivitet
}

data class SykmeldingJsonbMedisinskArsak(val isMedisinskArsak: Boolean)

data class SykmeldingJsonbArbeidsrelatertArsak(
    val isArbeidsrelatertArsak: Boolean,
    val arbeidsrelaterteArsaker: List<ArbeidsrelatertArsakType>,
    val annenArbeidsrelatertArsak: String?,
)

object SykmeldingTable : Table("sykmelding") {
    val id = javaUUID("id")
    val idempotencyKey = javaUUID("idempotency_key")
    val rules = jacksonJsonb<SykmeldingJsonbRuleResult>("rules")
    val metaSource = text("meta_source")
    val metaMottatt = timestampWithTimeZone("meta_mottatt")
    val metaPasientIdent = text("meta_pasient_ident")
    val metaPasientNavn = text("meta_pasient_navn")
    val metaBehandlerHpr = text("meta_behandler_hpr")
    val metaBehandlerNavn = text("meta_behandler_navn")
    val metaOrgnummer = text("meta_orgnummer")
    val metaTelefonnummer = text("meta_telefonnummer")
    val valuesPasientenSkalSkjermes = bool("values_pasienten_skal_skjermes")
    val valuesSvangerskapsrelatert = bool("values_svangerskapsrelatert")
    val valuesAnnenFravarsgrunn = text("values_annen_fravarsgrunn").nullable()
    val valuesHoveddiagnose =
        jacksonJsonb<SykmeldingJsonbDiagnose>("values_hoveddiagnose").nullable()
    val valuesBidiagnoser =
        jacksonJsonb<List<SykmeldingJsonbDiagnose>>("values_bidiagnoser").nullable()
    val valuesAktivitet = jacksonJsonb<List<SykmeldingJsonbAktivitet>>("values_aktivitet")
    val valuesMeldinger = jacksonJsonb<SykmeldingJsonbMeldinger>("values_meldinger").nullable()
    val valuesYrkesskade = jacksonJsonb<SykmeldingJsonbYrkesskade>("values_yrkesskade").nullable()
    val valuesArbeidsgiver =
        jacksonJsonb<SykmeldingJsonbArbeidsgiver>("values_arbeidsgiver").nullable()
    val valuesTilbakedatering =
        jacksonJsonb<SykmeldingJsonbTilbakedatering>("values_tilbakedatering").nullable()
    val valuesUtdypendeSporsmal = jsonb("values_utdypende_sporsmal", { it }, { it }).nullable()

    override val primaryKey = PrimaryKey(id)
}

private inline fun <reified Type : Any> Table.jacksonJsonb(name: String): Column<Type> {
    val writer = exposedJacksonObjectMapper.writerFor(object : TypeReference<Type>() {})

    return jsonb(
        name,
        { writer.writeValueAsString(it) },
        { exposedJacksonObjectMapper.readValue<Type>(it) },
    )
}
