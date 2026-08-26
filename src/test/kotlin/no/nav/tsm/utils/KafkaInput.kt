package no.nav.tsm.utils

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingAktivitet
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingFull
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskHenvisningRecord
import no.nav.tsm.sykmelding.input.core.model.*
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue

object KafkaTestConsumer {
    const val INPUT_TOPIC = "tsm.sykmeldinger-input"
    const val PIK_TOPIC = "tsm.pik"

    private val mapper = jacksonMapperBuilder().addModules(SykmeldingModule()).build()

    fun parseSykmeldingRecord(record: ByteArray?): SykmeldingRecord? =
        if (record != null) mapper.readValue(record) else null

    fun parsePIKRecord(record: ByteArray?): JuridiskHenvisningRecord? =
        if (record != null) mapper.readValue(record) else null

    fun createTestConsumer(config: Map<String, String>): KafkaConsumer<String, ByteArray?> {
        val kafkaProperties =
            config +
                mutableMapOf(
                    ConsumerConfig.GROUP_ID_CONFIG to "syk-inn-api-tests",
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "true",
                )

        val kafkaConsumer =
            KafkaConsumer(kafkaProperties, StringDeserializer(), ByteArrayDeserializer())

        kafkaConsumer.subscribe(listOf(INPUT_TOPIC, PIK_TOPIC))

        return kafkaConsumer
    }
}

object KafkaTestUtils {
    fun expectAllValues(sykmelding: BehandlerSykmeldingFull, record: SykmeldingRecord?) {
        requireNotNull(record) { "Record can't be null here lol" }
        sykmelding.sykmeldingId.toString() shouldBe record.sykmelding.id
        val sykmeldingRecord = record.shouldBeInstanceOf<SykmeldingRecord.Digital>()
        val digitalSykmelding = sykmeldingRecord.sykmelding.shouldBeInstanceOf<Sykmelding.Digital>()
        val digitalMetadata =
            sykmeldingRecord.metadata.shouldBeInstanceOf<MessageMetadata.Digital>()

        assertSoftly {
            // meta
            sykmelding.meta.pasient.ident shouldBe digitalSykmelding.pasient.fnr
            sykmelding.meta.sykmelder.hpr shouldBe
                digitalSykmelding.behandler.ids.firstOrNull { it.type == PersonIdType.HPR }?.id
            sykmelding.meta.legekontorOrgnr shouldBe digitalMetadata.orgnummer
            sykmelding.meta.legekontorTlf shouldBe
                digitalSykmelding.behandler.kontaktinfo
                    .firstOrNull { it.type == KontaktinfoType.TLF }
                    ?.value

            val medisinskVurdering =
                digitalSykmelding.medisinskVurdering.shouldBeInstanceOf<
                    MedisinskVurdering.Digital
                >()
            sykmelding.values.pasientenSkalSkjermes shouldBe medisinskVurdering.skjermetForPasient
            sykmelding.values.svangerskapsrelatert shouldBe medisinskVurdering.svangerskap
            sykmelding.values.annenFravarsgrunn shouldBe medisinskVurdering.annenFravarsgrunn
            sykmelding.values.hoveddiagnose?.code shouldBe medisinskVurdering.hovedDiagnose?.kode
            (sykmelding.values.bidiagnoser ?: emptyList()).map { it.code } shouldBe
                medisinskVurdering.biDiagnoser?.map { it.kode }
            sykmelding.values.yrkesskade?.skadedato shouldBe
                medisinskVurdering.yrkesskade?.yrkesskadeDato

            // aktivitet
            sykmelding.values.aktivitet shouldHaveSize digitalSykmelding.aktivitet.size
            sykmelding.values.aktivitet.zip(digitalSykmelding.aktivitet).forEach {
                (expected, actual) ->
                expected.fom shouldBe actual.fom
                expected.tom shouldBe actual.tom

                // TODO: Better asserts for other types of aktivitet

                if (expected is BehandlerSykmeldingAktivitet.Gradert) {
                    val actualGradert = actual.shouldBeInstanceOf<Aktivitet.Gradert>()

                    expected.grad shouldBe actualGradert.grad
                    expected.reisetilskudd shouldBe actualGradert.reisetilskudd
                }
            }

            if (sykmelding.values.arbeidsgiver?.harFlere == true) {
                // Har flere: We expect name and possibly meldingTilArbeidsgiver
                val ag = digitalSykmelding.arbeidsgiver.shouldBeInstanceOf<ArbeidsgiverInfo.Flere>()
                sykmelding.values.arbeidsgiver.arbeidsgivernavn shouldBe ag.navn
                sykmelding.values.meldinger?.tilArbeidsgiver shouldBe ag.meldingTilArbeidsgiver
            } else if (sykmelding.values.meldinger?.tilArbeidsgiver != null) {
                // Har arbeidsgiver, men ikke flere: We expect meldingTilArbeidsgiver, but not name
                val ag = digitalSykmelding.arbeidsgiver.shouldBeInstanceOf<ArbeidsgiverInfo.En>()
                sykmelding.values.meldinger.tilArbeidsgiver shouldBe ag.meldingTilArbeidsgiver
            } else {
                // No message, no arbeidsgiver - IngenArbeidsgiver
                digitalSykmelding.arbeidsgiver.shouldBeInstanceOf<ArbeidsgiverInfo.Ingen>()
            }

            // tilbakedatering
            sykmelding.values.tilbakedatering?.startdato shouldBe
                digitalSykmelding.tilbakedatering?.kontaktDato
            sykmelding.values.tilbakedatering?.begrunnelse shouldBe
                digitalSykmelding.tilbakedatering?.begrunnelse

            // meldinger
            sykmelding.values.meldinger?.tilNav shouldBe
                digitalSykmelding.bistandNav?.beskrivBistand

            // utfall
            sykmelding.utfall.result shouldBe record.validation.status

            // utdypendeSporsmal - check all svar values are present in record
            val recordSvar = digitalSykmelding.utdypendeSporsmal?.map { it.svar }.orEmpty()
            sykmelding.values.utdypendeSporsmal?.run {
                listOfNotNull(
                        utfordringerMedArbeid?.svar,
                        medisinskOppsummering?.svar,
                        hensynPaArbeidsplassen?.svar,
                        sykdomsutvikling?.svar,
                        arbeidsrelaterteUtfordringer?.svar,
                        behandlingOgFremtidigArbeid?.svar,
                        uavklarteForhold?.svar,
                        oppdatertMedisinskStatus?.svar,
                        realistiskMestringArbeid?.svar,
                        forventetHelsetilstandUtvikling?.svar,
                        medisinskeHensyn?.svar,
                    )
                    .forEach { svar -> recordSvar shouldContain svar }
            }
        }
    }
}
