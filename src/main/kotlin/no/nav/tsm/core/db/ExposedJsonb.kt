package no.nav.tsm.core.db

import kotlin.reflect.KClass
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingJsonbAktivitet
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingJsonbAktivitetType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.jsonb
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue

private abstract class CustomDeserializer<T : Any> : ValueDeserializer<T>() {
    abstract fun getClass(type: String): KClass<out T>

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T {
        val json = ctxt.readTree(p)
        val type = json.get("type").asString()
        val clazz = getClass(type)
        return ctxt.readTreeAsValue(json, clazz.java)
    }
}

private class SykmeldingJsonbAktivitetDeserializer :
    CustomDeserializer<SykmeldingJsonbAktivitet>() {
    override fun getClass(type: String): KClass<out SykmeldingJsonbAktivitet> {
        return when (SykmeldingJsonbAktivitetType.valueOf(type)) {
            SykmeldingJsonbAktivitetType.AKTIVITET_IKKE_MULIG ->
                SykmeldingJsonbAktivitet.IkkeMulig::class
            SykmeldingJsonbAktivitetType.GRADERT -> SykmeldingJsonbAktivitet.Gradert::class
            SykmeldingJsonbAktivitetType.AVVENTENDE -> SykmeldingJsonbAktivitet.Avventende::class
            SykmeldingJsonbAktivitetType.BEHANDLINGSDAGER ->
                SykmeldingJsonbAktivitet.Behandlingsdager::class
            SykmeldingJsonbAktivitetType.REISETILSKUDD ->
                SykmeldingJsonbAktivitet.Reisetilskudd::class
        }
    }
}

private class SykmeldingJsonbAktivitetModule : SimpleModule() {
    init {
        addDeserializer(
            SykmeldingJsonbAktivitet::class.java,
            SykmeldingJsonbAktivitetDeserializer(),
        )
    }
}

val exposedJacksonObjectMapper =
    jacksonMapperBuilder().addModules(SykmeldingJsonbAktivitetModule()).build()

inline fun <reified Type : Any> Table.jacksonJsonb(name: String): Column<Type> {
    return jsonb(
        name,
        { exposedJacksonObjectMapper.writeValueAsString(it) },
        { exposedJacksonObjectMapper.readValue<Type>(it) },
    )
}
