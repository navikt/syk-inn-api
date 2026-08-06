package no.nav.tsm.utils

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmelding
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingAktivitet
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingFull
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingRedacted
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.module.SimpleModule

fun ApplicationTestBuilder.testClient(): HttpClient {
    return createClient {
        install(ContentNegotiation) { jackson { addModules(BehandlerSykmeldingModule()) } }
    }
}

class BehandlerSykmeldingModule : SimpleModule() {
    init {
        addDeserializer(BehandlerSykmelding::class.java, BehandlerSykmeldingUnionDeserializer())
        addDeserializer(
            BehandlerSykmeldingAktivitet::class.java,
            BehandlerSykmeldingAktivitetUnionDeserializer(),
        )
    }
}

class BehandlerSykmeldingUnionDeserializer : ValueDeserializer<BehandlerSykmelding>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BehandlerSykmelding {
        val json = ctxt.readTree(p)
        val type =
            when (json["isFull"].booleanValue()) {
                false -> BehandlerSykmeldingRedacted::class
                true -> BehandlerSykmeldingFull::class
            }

        return ctxt.readTreeAsValue(json, type.java)
    }
}

class BehandlerSykmeldingAktivitetUnionDeserializer :
    ValueDeserializer<BehandlerSykmeldingAktivitet>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): BehandlerSykmeldingAktivitet {
        val json = ctxt.readTree(p)
        val discriminator = json["type"].asString()
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

        return ctxt.readTreeAsValue(json, subclassType.java)
    }
}
