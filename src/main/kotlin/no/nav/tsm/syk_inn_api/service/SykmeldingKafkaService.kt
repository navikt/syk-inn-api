package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.mottak.sykmelding.model.metadata.HelsepersonellKategori
import no.nav.tsm.mottak.sykmelding.model.metadata.MessageMetadata
import no.nav.tsm.mottak.sykmelding.model.metadata.Navn
import no.nav.tsm.mottak.sykmelding.model.metadata.PersonId
import no.nav.tsm.mottak.sykmelding.model.metadata.PersonIdType
import no.nav.tsm.syk_inn_api.model.PdlPerson
import no.nav.tsm.syk_inn_api.model.ValidationResult
import no.nav.tsm.syk_inn_api.model.sykmelding.Aktivitet
import no.nav.tsm.syk_inn_api.model.sykmelding.Hoveddiagnose
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.AktivitetIkkeMulig
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.AktivitetKafka
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Avventende
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Behandler
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Behandlingsdager
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.DiagnoseInfo
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.DigitalSykmeldingMetadata
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Gradert
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.MedisinskVurdering
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Pasient
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Reisetilskudd
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykInnSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Sykmelder
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class SykmeldingKafkaService(private val kafkaProducer: KafkaProducer<String, SykmeldingRecord>) {

    private val logger = LoggerFactory.getLogger(SykmeldingKafkaService::class.java)
    val sykmeldingInputTopic = "tsm.sykmeldinger-input"
    val sykmeldingMedBehandlingsutfallTopic = "tsm.sykmeldinger"
    fun send(
        payload: SykmeldingPayload,
        sykmeldingId: String,
        pdlPerson: PdlPerson,
        sykmelder: no.nav.tsm.syk_inn_api.model.Sykmelder,
    ) {
        try {
            val sykmeldingKafkaMessage = SykmeldingRecord(
                metadata = mapMessageMetadata(payload, sykmeldingId),
                sykmelding = mapToSykInnSykmelding(payload, sykmeldingId, pdlPerson, sykmelder),
                validation = mapValidationResult(),
            )
            // TODO implement sending to Kafka
            println("Sending sykmelding with id=$sykmeldingId to Kafka")
            val res = kafkaProducer.send(
                ProducerRecord(
                    sykmeldingInputTopic,
                    sykmeldingKafkaMessage,
                ),
            ).get()

            logger.info("Sent sykmelding with id=$sykmeldingId to Kafka")
        } catch (e: Exception) {
            println("Failed to send sykmelding with id=$sykmeldingId to Kafka")
            e.printStackTrace()
        }
    }

    private fun mapValidationResult(): ValidationResult {
        TODO("Not yet implemented")
    }

    private fun mapMessageMetadata(
        payload: SykmeldingPayload,
        sykmeldingId: String
    ): MessageMetadata {
        TODO("Not yet implemented")
    }

    fun mapToSykInnSykmelding(
        payload: SykmeldingPayload,
        sykmeldingId: String,
        pdlPerson: PdlPerson,
        sykmelder: no.nav.tsm.syk_inn_api.model.Sykmelder
    ): SykInnSykmelding {
        requireNotNull(sykmelder.fornavn)
        requireNotNull(sykmelder.etternavn)
        val helsepersonellkategoriKode = sykmelder.godkjenninger.first().helsepersonellkategori
        requireNotNull(helsepersonellkategoriKode)

        return SykInnSykmelding(
            id = sykmeldingId,
            metadata = DigitalSykmeldingMetadata(
                mottattDato = OffsetDateTime.now(),
                genDate = OffsetDateTime.now(),
            ),
            pasient = Pasient(
                navn = pdlPerson.navn,
                fnr = payload.pasientFnr,
                kontaktinfo = emptyList(),
            ),
            medisinskVurdering = mapMedisinskVurdering(payload),
            aktivitetKafka = mapAktivitet(payload),
            behandler = Behandler(
                //TODO treng vi en Behandler type i tillegg til Sykmelder i payloaden?
                navn = Navn(
                    fornavn = sykmelder.fornavn,
                    mellomnavn = sykmelder.mellomnavn,
                    etternavn = sykmelder.etternavn,
                ),
                ids = mapPersonIdsForSykmelder(sykmelder),
                kontaktinfo = emptyList(),
            ),
            sykmelder = Sykmelder(
                ids = mapPersonIdsForSykmelder(sykmelder),
                helsepersonellKategori = HelsepersonellKategori.parse(helsepersonellkategoriKode.verdi) // TODO er det rett verdi ??
            ),
        )
    }

    private fun mapPersonIdsForSykmelder(sykmelder: no.nav.tsm.syk_inn_api.model.Sykmelder): List<PersonId> {
        requireNotNull(sykmelder.hprNummer)
        requireNotNull(sykmelder.fnr)
        listOf(
            PersonId(
                id = sykmelder.hprNummer,
                type = PersonIdType.HPR,
            ),
            PersonId(
                id = sykmelder.fnr,
                type = PersonIdType.FNR,
            ),
        )
    }

    fun mapMedisinskVurdering(payload: SykmeldingPayload): MedisinskVurdering {
        return MedisinskVurdering(
            hovedDiagnose = mapHoveddiagnose(payload.sykmelding.hoveddiagnose),
            biDiagnoser = emptyList(), //TODO vi må støtte bidiagnoser inn i payload
            svangerskap = false, //TODO må få inn i payload
            skjermetForPasient = false, //TODO må få inn i payload
        ),
    }

    fun mapAktivitet(payload: SykmeldingPayload): List<AktivitetKafka> {
        return listOf(
            when (payload.sykmelding.aktivitet) {
                is Aktivitet.Gradert -> {
                    Gradert(
                        grad = payload.sykmelding.aktivitet.grad,
                        fom = LocalDate.parse(payload.sykmelding.aktivitet.fom),
                        tom = LocalDate.parse(payload.sykmelding.aktivitet.tom),
                        reisetilskudd = false, //TODO må få inn i payload - korleis veit vi om reisetilskudd
                    )
                }

                is Aktivitet.IkkeMulig -> {
                    AktivitetIkkeMulig(
                        fom = LocalDate.parse(payload.sykmelding.aktivitet.fom),
                        tom = LocalDate.parse(payload.sykmelding.aktivitet.tom),
                    )
                }

                is Aktivitet.Avventende -> {
                    Avventende(
                        innspillTilArbeidsgiver = payload.sykmelding.aktivitet.innspillTilArbeidsgiver,
                        fom = LocalDate.parse(payload.sykmelding.aktivitet.fom),
                        tom = LocalDate.parse(payload.sykmelding.aktivitet.tom),
                    )
                }

                is Aktivitet.Behandlingsdager -> {
                    Behandlingsdager(
                        antallBehandlingsdager = payload.sykmelding.aktivitet.antallBehandlingsdager,
                        fom = LocalDate.parse(payload.sykmelding.aktivitet.fom),
                        tom = LocalDate.parse(payload.sykmelding.aktivitet.tom),
                    )
                }

                is Aktivitet.Reisetilskudd -> {
                    Reisetilskudd(
                        fom = LocalDate.parse(payload.sykmelding.aktivitet.fom),
                        tom = LocalDate.parse(payload.sykmelding.aktivitet.tom),
                    )
                }
            },
        )
    }

    fun mapHoveddiagnose(hoveddiagnose: Hoveddiagnose): DiagnoseInfo {
        return DiagnoseInfo(
            system = hoveddiagnose.system,
            kode = hoveddiagnose.code,
        )
    }
}
