package no.nav.tsm.utils

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.Properties
import kotlin.collections.set
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingFull
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskHenvisningRecord
import no.nav.tsm.sykmelding.input.core.model.DigitalMedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.EnArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.FlereArbeidsgivere
import no.nav.tsm.sykmelding.input.core.model.IngenArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.metadata.Digital
import no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.testcontainers.kafka.ConfluentKafkaContainer

object KafkaTestConsumer {
    const val INPUT_TOPIC = "tsm.sykmeldinger-input"
    const val PIK_TOPIC = "teamsykmelding.paragraf-i-kode"

    private val mapper =
        jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerModule(SykmeldingModule())
        }

    fun parseSykmeldingRecord(record: ByteArray?): SykmeldingRecord? =
        if (record != null) mapper.readValue(record) else null

    fun parsePIKRecord(record: ByteArray?): JuridiskHenvisningRecord? =
        if (record != null) mapper.readValue(record) else null

    fun createTestConsumer(container: ConfluentKafkaContainer): KafkaConsumer<String, ByteArray?> {
        val kafkaProperties =
            Properties().apply { this["bootstrap.servers"] = container.bootstrapServers }

        kafkaProperties.apply {
            this[ConsumerConfig.GROUP_ID_CONFIG] = "syk-inn-api-tests"
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        }

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

        val digitalSykmelding = record.sykmelding.shouldBeInstanceOf<DigitalSykmelding>()
        val digitalMetadata = record.metadata.shouldBeInstanceOf<Digital>()

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
                digitalSykmelding.medisinskVurdering.shouldBeInstanceOf<DigitalMedisinskVurdering>()
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
            }

            if (sykmelding.values.arbeidsgiver?.harFlere == true) {
                // Har flere: We expect name and possibly meldingTilArbeidsgiver
                val ag = digitalSykmelding.arbeidsgiver.shouldBeInstanceOf<FlereArbeidsgivere>()
                sykmelding.values.arbeidsgiver.arbeidsgivernavn shouldBe ag.navn
                sykmelding.values.meldinger?.tilArbeidsgiver shouldBe ag.meldingTilArbeidsgiver
            } else if (sykmelding.values.meldinger?.tilArbeidsgiver != null) {
                // Har arbeidsgiver, men ikke flere: We expect meldingTilArbeidsgiver, but not name
                val ag = digitalSykmelding.arbeidsgiver.shouldBeInstanceOf<EnArbeidsgiver>()
                sykmelding.values.meldinger.tilArbeidsgiver shouldBe ag.meldingTilArbeidsgiver
            } else {
                // No message, no arbeidsgiver - IngenArbeidsgiver
                digitalSykmelding.arbeidsgiver.shouldBeInstanceOf<IngenArbeidsgiver>()
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
