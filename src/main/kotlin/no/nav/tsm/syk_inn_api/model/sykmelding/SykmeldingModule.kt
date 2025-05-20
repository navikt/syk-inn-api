package no.nav.tsm.syk_inn_api.model.sykmelding

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.tsm.mottak.sykmelding.model.metadata.Digital
import no.nav.tsm.mottak.sykmelding.model.metadata.EDIEmottak
import no.nav.tsm.mottak.sykmelding.model.metadata.Egenmeldt
import no.nav.tsm.mottak.sykmelding.model.metadata.EmottakEnkel
import no.nav.tsm.mottak.sykmelding.model.metadata.MessageMetadata
import no.nav.tsm.mottak.sykmelding.model.metadata.MetadataType
import no.nav.tsm.mottak.sykmelding.model.metadata.Papir
import no.nav.tsm.mottak.sykmelding.model.metadata.Utenlandsk
import no.nav.tsm.syk_inn_api.model.InvalidRule
import no.nav.tsm.syk_inn_api.model.OKRule
import no.nav.tsm.syk_inn_api.model.PendingRule
import no.nav.tsm.syk_inn_api.model.Rule
import no.nav.tsm.syk_inn_api.model.RuleType
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.ARBEIDSGIVER_TYPE
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.AktivitetIkkeMulig
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.AktivitetKafka
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Aktivitetstype
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.ArbeidsgiverInfo
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Avventende
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Behandlingsdager
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.DigitalSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.DigitalSykmeldingMetadata
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.EnArbeidsgiver
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.ErIArbeid
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.ErIkkeIArbeid
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.FlereArbeidsgivere
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Gradert
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.IArbeid
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.IArbeidType
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.ISykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.IngenArbeidsgiver
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.OldSykmeldingMetadata
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Papirsykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Reisetilskudd
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingMeta
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingType
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.UtenlandskSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.XmlSykmelding
import kotlin.reflect.KClass


class SykmeldingModule : SimpleModule() {
    init {
        addDeserializer(ISykmelding::class.java, SykmeldingDeserializer())
        addDeserializer(AktivitetKafka::class.java, AktivitetDeserializer())
        addDeserializer(ArbeidsgiverInfo::class.java, ArbeidsgiverInfoDeserializer())
        addDeserializer(IArbeid::class.java, IArbeidDeserializer())
        addDeserializer(Rule::class.java, RuleDeserializer())
        addDeserializer(MessageMetadata::class.java, MeldingsinformasjonDeserializer())
        addDeserializer(SykmeldingMeta::class.java, SykmeldingMetaDeserializer())
    }
}


abstract class CustomDeserializer<T : Any> : JsonDeserializer<T>() {
    abstract fun getClass(type: String): KClass<out T>

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T {
        val node: ObjectNode = p.codec.readTree(p)
        val type = node.get("type").asText()
        val clazz = getClass(type)
        return p.codec.treeToValue(node, clazz.java)
    }
}

class SykmeldingDeserializer : CustomDeserializer<ISykmelding>() {
    override fun getClass(type: String): KClass<out ISykmelding> {
        return when (SykmeldingType.valueOf(type)) {
            SykmeldingType.XML -> XmlSykmelding::class
            SykmeldingType.PAPIR -> Papirsykmelding::class
            SykmeldingType.UTENLANDSK -> UtenlandskSykmelding::class
            SykmeldingType.DIGITAL -> DigitalSykmelding::class
        }
    }
}

class MeldingsinformasjonDeserializer : CustomDeserializer<MessageMetadata>() {
    override fun getClass(type: String): KClass<out MessageMetadata> {
        return when (MetadataType.valueOf(type)) {
            MetadataType.ENKEL -> EmottakEnkel::class
            MetadataType.EMOTTAK -> EDIEmottak::class
            MetadataType.UTENLANDSK_SYKMELDING -> Utenlandsk::class
            MetadataType.PAPIRSYKMELDING -> Papir::class
            MetadataType.EGENMELDT -> Egenmeldt::class
            MetadataType.DIGITAL -> Digital::class
        }
    }
}

class RuleDeserializer : CustomDeserializer<Rule>() {

    override fun getClass(type: String): KClass<out Rule> {
        return when (RuleType.valueOf(type)) {
            RuleType.INVALID -> InvalidRule::class
            RuleType.PENDING -> PendingRule::class
            RuleType.OK -> OKRule::class
        }
    }
}

class IArbeidDeserializer : CustomDeserializer<IArbeid>() {
    override fun getClass(type: String): KClass<out IArbeid> {
        return when (IArbeidType.valueOf(type)) {
            IArbeidType.ER_I_ARBEID -> ErIArbeid::class
            IArbeidType.ER_IKKE_I_ARBEID -> ErIkkeIArbeid::class
        }
    }
}


class ArbeidsgiverInfoDeserializer : CustomDeserializer<ArbeidsgiverInfo>() {
    override fun getClass(type: String): KClass<out ArbeidsgiverInfo> {
        return when (ARBEIDSGIVER_TYPE.valueOf(type)) {
            ARBEIDSGIVER_TYPE.EN_ARBEIDSGIVER -> EnArbeidsgiver::class
            ARBEIDSGIVER_TYPE.FLERE_ARBEIDSGIVERE -> FlereArbeidsgivere::class
            ARBEIDSGIVER_TYPE.INGEN_ARBEIDSGIVER -> IngenArbeidsgiver::class
        }
    }
}

class AktivitetDeserializer : CustomDeserializer<AktivitetKafka>() {
    override fun getClass(type: String): KClass<out AktivitetKafka> {
        return when (Aktivitetstype.valueOf(type)) {
            Aktivitetstype.AKTIVITET_IKKE_MULIG -> AktivitetIkkeMulig::class
            Aktivitetstype.AVVENTENDE -> Avventende::class
            Aktivitetstype.BEHANDLINGSDAGER -> Behandlingsdager::class
            Aktivitetstype.GRADERT -> Gradert::class
            Aktivitetstype.REISETILSKUDD -> Reisetilskudd::class
        }
    }
}

class SykmeldingMetaDeserializer : CustomDeserializer<SykmeldingMeta>() {
    override fun getClass(type: String): KClass<out SykmeldingMeta> {
        return when (SykmeldingType.valueOf(type)) {
            SykmeldingType.XML -> OldSykmeldingMetadata::class
            SykmeldingType.PAPIR -> OldSykmeldingMetadata::class
            SykmeldingType.UTENLANDSK -> OldSykmeldingMetadata::class
            SykmeldingType.DIGITAL -> DigitalSykmeldingMetadata::class
        }
    }
}
