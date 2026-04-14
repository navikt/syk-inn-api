package no.nav.tsm.modules.sykmeldinger.db.sykmelding

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.core.db.exposedJacksonObjectMapper
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import org.jetbrains.exposed.v1.json.jsonb

object SykmeldingTable : Table("sykmelding") {
    val id = javaUUID("id")
    val idempotencyKey = javaUUID("idempotency_key")
    val rules = jacksonJsonb<SykmeldingJsonbRuleResult>("rules")
    val metaSource = text("meta_source")
    val metaMottatt = timestampWithTimeZone("meta_mottatt")
    val metaPasientIdent = text("meta_pasient_ident")
    val metaPasientNavn = jacksonJsonb<SykmeldingJsonbNavn>("meta_pasient_navn")
    val metaBehandlerHpr = text("meta_behandler_hpr")
    val metaBehandlerHelsepersonellkategori =
        jacksonJsonb<List<String>>("meta_behandler_helsepersonellkategori")
    val metaBehandlerNavn = jacksonJsonb<SykmeldingJsonbNavn>("meta_behandler_navn")
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
    val valuesUtdypendeSporsmal =
        jacksonJsonb<Map<String, SykmeldingJsonbUtdypendeSporsmal>>("values_utdypende_sporsmal")
            .nullable()

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
