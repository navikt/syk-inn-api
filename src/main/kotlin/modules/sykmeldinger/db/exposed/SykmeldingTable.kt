@file:OptIn(ExperimentalUuidApi::class)

package no.nav.tsm.modules.sykmeldinger.db.exposed

import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.uuid.ExperimentalUuidApi
import no.nav.tsm.sykmelding.input.core.model.RuleType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import org.jetbrains.exposed.v1.json.jsonb

data class SykmeldingColumnRuleResult(val type: RuleType, val message: String?, val rule: String?)

object SykmeldingTable : Table("sykmelding") {
    val id = javaUUID("id")
    val rules =
        jsonb<SykmeldingColumnRuleResult>(
            "rules",
            { exposedJacksonObjectMapper.writeValueAsString(it) },
            { exposedJacksonObjectMapper.readValue(it) },
        )
    val metaSource = text("meta_source")
    val metaMottatt = timestampWithTimeZone("meta_mottatt")
    val metaPasientIdent = text("meta_pasient_ident")
    val metaBehandlerHpr = text("meta_behandler_hpr")
    val metaOrgnummer = text("meta_orgnummer")
    val metaTelefonnummer = text("meta_telefonnummer")
    val valuesPasientenSkalSkjermes = bool("values_pasienten_skal_skjermes")
    val valuesHoveddiagnose = jsonb("values_hoveddiagnose", { it }, { it }).nullable()
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
