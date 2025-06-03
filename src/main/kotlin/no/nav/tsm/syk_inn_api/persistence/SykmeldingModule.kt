package no.nav.tsm.syk_inn_api.persistence

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlin.reflect.KClass
import no.nav.tsm.mottak.sykmelding.model.metadata.Digital
import no.nav.tsm.mottak.sykmelding.model.metadata.EDIEmottak
import no.nav.tsm.mottak.sykmelding.model.metadata.Egenmeldt
import no.nav.tsm.mottak.sykmelding.model.metadata.EmottakEnkel
import no.nav.tsm.mottak.sykmelding.model.metadata.MessageMetadata
import no.nav.tsm.mottak.sykmelding.model.metadata.MetadataType
import no.nav.tsm.mottak.sykmelding.model.metadata.Papir
import no.nav.tsm.mottak.sykmelding.model.metadata.Utenlandsk
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.ARBEIDSGIVER_TYPE
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.ArbeidsgiverInfo
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.DigitalSykmelding
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.DigitalSykmeldingMetadata
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.EnArbeidsgiver
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.ErIArbeid
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.ErIkkeIArbeid
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.FlereArbeidsgivere
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.IArbeid
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.IArbeidType
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.ISykmelding
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.IngenArbeidsgiver
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.OldSykmeldingMetadata
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.Papirsykmelding
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.SykmeldingMeta
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.SykmeldingRecordAktivitet
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.SykmeldingRecordAktivitetsType
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.SykmeldingType
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.UtenlandskSykmelding
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.XmlSykmelding
import no.nav.tsm.syk_inn_api.rules.InvalidRule
import no.nav.tsm.syk_inn_api.rules.OKRule
import no.nav.tsm.syk_inn_api.rules.PendingRule
import no.nav.tsm.syk_inn_api.rules.Rule
import no.nav.tsm.syk_inn_api.rules.RuleType

class SykmeldingModule : SimpleModule() {
    init {
        addDeserializer(ISykmelding::class.java, SykmeldingDeserializer())
        addDeserializer(SykmeldingRecordAktivitet::class.java, AktivitetDeserializer())
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

class AktivitetDeserializer : CustomDeserializer<SykmeldingRecordAktivitet>() {
    override fun getClass(type: String): KClass<out SykmeldingRecordAktivitet> {
        return when (SykmeldingRecordAktivitetsType.valueOf(type)) {
            SykmeldingRecordAktivitetsType.AKTIVITET_IKKE_MULIG ->
                SykmeldingRecordAktivitet.AktivitetIkkeMulig::class
            SykmeldingRecordAktivitetsType.AVVENTENDE -> SykmeldingRecordAktivitet.Avventende::class
            SykmeldingRecordAktivitetsType.BEHANDLINGSDAGER ->
                SykmeldingRecordAktivitet.Behandlingsdager::class
            SykmeldingRecordAktivitetsType.GRADERT -> SykmeldingRecordAktivitet.Gradert::class
            SykmeldingRecordAktivitetsType.REISETILSKUDD ->
                SykmeldingRecordAktivitet.Reisetilskudd::class
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
