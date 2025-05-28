package no.nav.tsm.syk_inn_api.service

import java.time.LocalDate
import java.time.OffsetDateTime
import no.nav.tsm.mottak.sykmelding.model.metadata.Digital
import no.nav.tsm.mottak.sykmelding.model.metadata.HelsepersonellKategori
import no.nav.tsm.mottak.sykmelding.model.metadata.MessageMetadata
import no.nav.tsm.mottak.sykmelding.model.metadata.Navn
import no.nav.tsm.mottak.sykmelding.model.metadata.PersonId
import no.nav.tsm.mottak.sykmelding.model.metadata.PersonIdType
import no.nav.tsm.regulus.regula.RegulaOutcomeStatus
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.exception.PersonNotFoundException
import no.nav.tsm.syk_inn_api.exception.SykmeldingDBMappingException
import no.nav.tsm.syk_inn_api.model.InvalidRule
import no.nav.tsm.syk_inn_api.model.OKRule
import no.nav.tsm.syk_inn_api.model.PdlPerson
import no.nav.tsm.syk_inn_api.model.PendingRule
import no.nav.tsm.syk_inn_api.model.Reason
import no.nav.tsm.syk_inn_api.model.RuleType
import no.nav.tsm.syk_inn_api.model.ValidationResult
import no.nav.tsm.syk_inn_api.model.ValidationType
import no.nav.tsm.syk_inn_api.model.sykmelding.Aktivitet
import no.nav.tsm.syk_inn_api.model.sykmelding.Hoveddiagnose
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.AktivitetIkkeMulig
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.AktivitetKafka
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Avventende
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Behandler
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Behandlingsdager
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.DiagnoseInfo
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.DigitalSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.DigitalSykmeldingMetadata
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Gradert
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.MedisinskVurdering
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Pasient
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Reisetilskudd
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.Sykmelder
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class SykmeldingKafkaService(
    private val kafkaProducer: KafkaProducer<String, SykmeldingRecord>,
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
    @Value("\${nais.cluster}") private val clusterName: String,
) {
    @Value("\${topics.write}") private lateinit var sykmeldingInputTopic: String
    private val logger = LoggerFactory.getLogger(SykmeldingKafkaService::class.java)

    fun send(
        payload: SykmeldingPayload,
        sykmeldingId: String,
        pdlPerson: PdlPerson,
        sykmelder: no.nav.tsm.syk_inn_api.model.Sykmelder,
        regulaResult: RegulaResult,
    ) {
        try {
            val sykmeldingKafkaMessage =
                SykmeldingRecord(
                    metadata = mapMessageMetadata(payload),
                    sykmelding =
                        mapToDigitalSykmelding(payload, sykmeldingId, pdlPerson, sykmelder),
                    validation = mapValidationResult(regulaResult),
                )
            kafkaProducer
                .send(
                    ProducerRecord(
                        sykmeldingInputTopic,
                        sykmeldingId,
                        sykmeldingKafkaMessage,
                    ),
                )
                .get()

            logger.info("Sent sykmelding with id=$sykmeldingId to Kafka")
        } catch (e: Exception) {
            logger.error("Failed to send sykmelding with id=$sykmeldingId to Kafka")
            e.printStackTrace()
        }
    }

    @KafkaListener(
        topics = ["\${topics.read}"],
        groupId = "syk-inn-api-consumer",
        containerFactory = "kafkaListenerContainerFactory",
        batch = "false"
    )
    fun consume(record: ConsumerRecord<String, SykmeldingRecord>) {
        try {
            //            if (record.key() == null) {
            //                logger.warn("Record key is null, skipping record")
            //                acknowledgement.acknowledge()
            //                return
            //            }
            logger.info(
                "Consuming record: ${record.value()} from topic ${record.topic()}"
            ) // TODO logg i sikker logg før prod. DO IT!
            sykmeldingPersistenceService.updateSykmelding(record.key(), record.value())
        } catch (e: PersonNotFoundException) {
            logger.error("Failed to process sykmelding with id ${record.key()} . Person not found in Pdl Exception", e)
            if (clusterName == "dev-gcp") {
                logger.warn("Person not found in dev-gcp, skipping sykmelding")
            } else {
                throw e
            }
        } catch (e: SykmeldingDBMappingException) {
            logger.error("Failed to process sykmelding with id ${record.key()} . Failed to map sykmelding exception" , e)
            if (clusterName == "dev-gcp") {
                logger.warn("Failed to map sykmelding in dev-gcp, skipping sykmelding")
            } else {
                throw e
            }
        } catch (e: Exception) {
            logger.error("Failed to process sykmelding with id ${record.key()} . Generic exception", e)
            throw e
        }
    }

    private fun mapValidationResult(regulaResult: RegulaResult): ValidationResult {
        val rule =
            when (regulaResult) {
                is RegulaResult.Ok -> {
                    OKRule(
                        name = RuleType.OK.name,
                        timestamp = OffsetDateTime.now(),
                        validationType = ValidationType.AUTOMATIC
                    )
                }
                is RegulaResult.NotOk -> {
                    when (regulaResult.outcome.status) {
                        RegulaOutcomeStatus.MANUAL_PROCESSING ->
                            PendingRule(
                                name = RuleType.PENDING.name,
                                reason =
                                    Reason(
                                        sykmeldt = regulaResult.outcome.reason.sykmeldt,
                                        sykmelder = regulaResult.outcome.reason.sykmelder,
                                    ),
                                timestamp = OffsetDateTime.now(),
                                validationType = ValidationType.MANUAL
                            )
                        RegulaOutcomeStatus.INVALID ->
                            InvalidRule(
                                name = RuleType.INVALID.name,
                                reason =
                                    Reason(
                                        sykmeldt = regulaResult.outcome.reason.sykmeldt,
                                        sykmelder = regulaResult.outcome.reason.sykmelder,
                                    ),
                                timestamp = OffsetDateTime.now(),
                                validationType = ValidationType.AUTOMATIC
                            )
                        else -> {
                            throw IllegalArgumentException(
                                "Unknown status: ${regulaResult.outcome.status}"
                            )
                        }
                    }
                }
            }

        val rules = listOf(rule)
        return ValidationResult(status = rule.type, timestamp = OffsetDateTime.now(), rules = rules)
    }

    private fun mapMessageMetadata(payload: SykmeldingPayload): MessageMetadata {
        return Digital(orgnummer = payload.legekontorOrgnr)
    }

    fun mapToDigitalSykmelding(
        payload: SykmeldingPayload,
        sykmeldingId: String,
        pdlPerson: PdlPerson,
        sykmelder: no.nav.tsm.syk_inn_api.model.Sykmelder
    ): DigitalSykmelding {
        requireNotNull(sykmelder.fornavn)
        requireNotNull(sykmelder.etternavn)
        val helsepersonellKategoriKode = sykmelder.godkjenninger.first().helsepersonellkategori
        requireNotNull(helsepersonellKategoriKode)

        return DigitalSykmelding(
            id = sykmeldingId,
            metadata =
                DigitalSykmeldingMetadata(
                    mottattDato = OffsetDateTime.now(),
                    genDate = OffsetDateTime.now(),
                ),
            pasient =
                Pasient(
                    navn = pdlPerson.navn,
                    fnr = payload.pasientFnr,
                    kontaktinfo = emptyList(),
                ),
            medisinskVurdering = mapMedisinskVurdering(payload),
            aktivitetKafka = mapAktivitet(payload),
            behandler =
                Behandler(
                    navn =
                        Navn(
                            fornavn = sykmelder.fornavn,
                            mellomnavn = sykmelder.mellomnavn,
                            etternavn = sykmelder.etternavn,
                        ),
                    ids = mapPersonIdsForSykmelder(sykmelder),
                    kontaktinfo = emptyList(),
                ),
            sykmelder =
                Sykmelder(
                    ids = mapPersonIdsForSykmelder(sykmelder),
                    helsepersonellKategori =
                        HelsepersonellKategori.parse(
                            helsepersonellKategoriKode.verdi
                        ), // TODO er det rett verdi ??
                ),
        )
    }

    private fun mapPersonIdsForSykmelder(
        sykmelder: no.nav.tsm.syk_inn_api.model.Sykmelder
    ): List<PersonId> {
        requireNotNull(sykmelder.hprNummer)
        requireNotNull(sykmelder.fnr)
        return listOf(
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
            biDiagnoser = emptyList(), // TODO vi må støtte bidiagnoser inn i payload
            svangerskap = false, // TODO må få inn i payload
            skjermetForPasient = false,
            yrkesskade = null,
            syketilfelletStartDato = null,
            annenFraversArsak = null,
        )
    }

    fun mapAktivitet(payload: SykmeldingPayload): List<AktivitetKafka> {
        return listOf(
            when (payload.sykmelding.aktivitet) {
                is Aktivitet.Gradert -> {
                    Gradert(
                        grad = payload.sykmelding.aktivitet.grad,
                        fom = LocalDate.parse(payload.sykmelding.aktivitet.fom),
                        tom = LocalDate.parse(payload.sykmelding.aktivitet.tom),
                        reisetilskudd = payload.sykmelding.aktivitet.reisetilskudd,
                    )
                }
                is Aktivitet.IkkeMulig -> {
                    AktivitetIkkeMulig(
                        fom = LocalDate.parse(payload.sykmelding.aktivitet.fom),
                        tom = LocalDate.parse(payload.sykmelding.aktivitet.tom),
                        medisinskArsak = null,
                        arbeidsrelatertArsak = null,
                    )
                }
                is Aktivitet.Avventende -> {
                    Avventende(
                        innspillTilArbeidsgiver =
                            payload.sykmelding.aktivitet.innspillTilArbeidsgiver,
                        fom = LocalDate.parse(payload.sykmelding.aktivitet.fom),
                        tom = LocalDate.parse(payload.sykmelding.aktivitet.tom),
                    )
                }
                is Aktivitet.Behandlingsdager -> {
                    Behandlingsdager(
                        antallBehandlingsdager =
                            payload.sykmelding.aktivitet.antallBehandlingsdager,
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
