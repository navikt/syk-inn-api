package no.nav.tsm.modules.sykmeldinger.db.exposed

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.sykmelding.input.core.model.RuleType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import org.jetbrains.exposed.v1.json.jsonb

data class SykmeldingJsonbRuleResult(val type: RuleType, val message: String?, val rule: String?)

data class SykmeldingJsonbDiagnose(val system: String, val text: String, val code: String)

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
    val valuesHoveddiagnose =
        jacksonJsonb<SykmeldingJsonbDiagnose>("values_hoveddiagnose").nullable()
    val valuesBidiagnoser = jsonb("values_bidiagnoser", { it }, { it })
    val valuesAktivitet = jsonb("values_aktivitet", { it }, { it })
    val valuesSvangerskapsrelatert = bool("values_svangerskapsrelatert")
    val valuesMeldinger = jsonb("values_meldinger", { it }, { it }).nullable()
    val valuesYrkesskade = jsonb("values_yrkesskade", { it }, { it }).nullable()
    val valuesArbeidsgiver = jsonb("values_arbeidsgiver", { it }, { it }).nullable()
    val valuesTilbakedatering = jsonb("values_tilbakedatering", { it }, { it }).nullable()
    val valuesUtdypendeSporsmal = jsonb("values_utdypende_sporsmal", { it }, { it }).nullable()
    val valuesAnnenFravarsgrunn = jsonb("values_annen_fravarsgrunn", { it }, { it }).nullable()

    override val primaryKey = PrimaryKey(id)
}

private inline fun <reified Type : Any> Table.jacksonJsonb(name: String) =
    jsonb(
        name,
        { exposedJacksonObjectMapper.writeValueAsString(it) },
        { exposedJacksonObjectMapper.readValue<Type>(it) },
    )
