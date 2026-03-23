package no.nav.tsm.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmelding
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingAktivitet
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingFull
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingRedacted

fun ApplicationTestBuilder.testClient(): HttpClient {
    return createClient {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                registerModule(SykmeldingModule())
            }
        }
    }
}

class SykmeldingModule : SimpleModule() {
    init {
        addDeserializer(BehandlerSykmelding::class.java, SykmeldingUnionDeserializer())
        addDeserializer(
            BehandlerSykmeldingAktivitet::class.java,
            SykmeldingAktivitetUnionDeserializer(),
        )
    }
}

class SykmeldingUnionDeserializer : JsonDeserializer<BehandlerSykmelding>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BehandlerSykmelding {
        val json = ctxt.readTree(p)
        val type =
            when (json["utfall"]) {
                null -> BehandlerSykmeldingRedacted::class
                else -> BehandlerSykmeldingFull::class
            }

        return p.codec.treeToValue(json, type.java)
    }
}

class SykmeldingAktivitetUnionDeserializer : JsonDeserializer<BehandlerSykmeldingAktivitet>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): BehandlerSykmeldingAktivitet {
        val json = ctxt.readTree(p)
        val discriminator = json["type"].asText()
        val type = BehandlerSykmeldingAktivitet.BehandlerSykmeldingType.valueOf(discriminator)

        val subclassType =
            when (type) {
                BehandlerSykmeldingAktivitet.BehandlerSykmeldingType.AKTIVITET_IKKE_MULIG ->
                    BehandlerSykmeldingAktivitet.IkkeMulig::class

                BehandlerSykmeldingAktivitet.BehandlerSykmeldingType.GRADERT ->
                    BehandlerSykmeldingAktivitet.Gradert::class

                BehandlerSykmeldingAktivitet.BehandlerSykmeldingType.AVVENTENDE ->
                    BehandlerSykmeldingAktivitet.Avventende::class

                BehandlerSykmeldingAktivitet.BehandlerSykmeldingType.BEHANDLINGSDAGER ->
                    BehandlerSykmeldingAktivitet.Behandlingsdager::class

                BehandlerSykmeldingAktivitet.BehandlerSykmeldingType.REISETILSKUDD ->
                    BehandlerSykmeldingAktivitet.Reisetilskudd::class
            }

        return p.codec.treeToValue(json, subclassType.java)
    }
}
