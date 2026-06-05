package no.nav.tsm.core.db

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.reflect.KClass
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingJsonbAktivitet
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingJsonbAktivitetType
import no.nav.tsm.sykmelding.input.core.model.CustomDeserializer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.jsonb

class SykmeldingJsonbAktivitetDeserializer : CustomDeserializer<SykmeldingJsonbAktivitet>() {
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

class SykmeldingJsonbAktivitetModule : SimpleModule() {
    init {
        addDeserializer(
            SykmeldingJsonbAktivitet::class.java,
            SykmeldingJsonbAktivitetDeserializer(),
        )
    }
}

val exposedJacksonObjectMapper =
    jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        registerModule(SykmeldingJsonbAktivitetModule())

        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

inline fun <reified Type : Any> Table.jacksonJsonb(name: String): Column<Type> {
    return jsonb(
        name,
        { exposedJacksonObjectMapper.writeValueAsString(it) },
        { exposedJacksonObjectMapper.readValue<Type>(it) },
    )
}
