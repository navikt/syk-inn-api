package no.nav.tsm.syk_inn_api.sykmelding.kafka

import java.time.LocalDate
import java.time.OffsetDateTime
import no.nav.tsm.regulus.regula.RegulaOutcomeStatus
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.Hoveddiagnose
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Digital
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.HelsepersonellKategori
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.MessageMetadata
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Navn
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.PersonId
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.PersonIdType
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.Behandler
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.DiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.DigitalSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.DigitalSykmeldingMetadata
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.Pasient
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordMedisinskVurdering
import no.nav.tsm.syk_inn_api.sykmelding.rules.InvalidRule
import no.nav.tsm.syk_inn_api.sykmelding.rules.OKRule
import no.nav.tsm.syk_inn_api.sykmelding.rules.PendingRule
import no.nav.tsm.syk_inn_api.sykmelding.rules.Reason
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleType
import no.nav.tsm.syk_inn_api.sykmelding.rules.ValidationResult
import no.nav.tsm.syk_inn_api.sykmelding.rules.ValidationType

object SykmeldingKafkaMapper {
    fun mapValidationResult(regulaResult: RegulaResult): ValidationResult {
        val rule =
            when (regulaResult) {
                is RegulaResult.Ok -> {
                    OKRule(
                        name = RuleType.OK.name,
                        timestamp = OffsetDateTime.now(),
                        validationType = ValidationType.AUTOMATIC,
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
                                validationType = ValidationType.MANUAL,
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
                                validationType = ValidationType.AUTOMATIC,
                            )
                        else -> {
                            throw IllegalArgumentException(
                                "Unknown status: ${regulaResult.outcome.status}",
                            )
                        }
                    }
                }
            }

        val rules = listOf(rule)
        return ValidationResult(status = rule.type, timestamp = OffsetDateTime.now(), rules = rules)
    }

    fun mapMessageMetadata(payload: SykmeldingPayload): MessageMetadata {
        return Digital(orgnummer = payload.legekontorOrgnr)
    }

    fun mapToDigitalSykmelding(
        payload: SykmeldingPayload,
        sykmeldingId: String,
        person: Person,
        sykmelder: HprSykmelder
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
                    navn = person.navn,
                    fnr = payload.pasientFnr,
                    kontaktinfo = emptyList(),
                ),
            sykmeldingRecordMedisinskVurdering = mapMedisinskVurdering(payload),
            sykmeldingRecordAktivitet = mapAktivitet(payload),
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
                            helsepersonellKategoriKode.verdi,
                        ), // TODO er det rett verdi ??
                ),
        )
    }

    private fun mapPersonIdsForSykmelder(sykmelder: HprSykmelder): List<PersonId> {
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

    fun mapMedisinskVurdering(payload: SykmeldingPayload): SykmeldingRecordMedisinskVurdering {
        return SykmeldingRecordMedisinskVurdering(
            hovedDiagnose = mapHoveddiagnose(payload.sykmelding.hoveddiagnose),
            biDiagnoser = emptyList(), // TODO vi må støtte bidiagnoser inn i payload
            svangerskap = false, // TODO må få inn i payload
            skjermetForPasient = false,
            yrkesskade = null,
            syketilfelletStartDato = null,
            annenFraversArsak = null,
        )
    }
    // TODO bør være egen mapper klasse for records  greier ?
    fun mapAktivitet(payload: SykmeldingPayload): List<SykmeldingRecordAktivitet> {
        return listOf(
            when (payload.sykmelding.aktivitet) {
                is OpprettSykmeldingAktivitet.Gradert -> {
                    SykmeldingRecordAktivitet.Gradert(
                        grad = payload.sykmelding.aktivitet.grad,
                        fom = LocalDate.parse(payload.sykmelding.aktivitet.fom),
                        tom = LocalDate.parse(payload.sykmelding.aktivitet.tom),
                        reisetilskudd = payload.sykmelding.aktivitet.reisetilskudd,
                    )
                }
                is OpprettSykmeldingAktivitet.IkkeMulig -> {
                    SykmeldingRecordAktivitet.AktivitetIkkeMulig(
                        fom = LocalDate.parse(payload.sykmelding.aktivitet.fom),
                        tom = LocalDate.parse(payload.sykmelding.aktivitet.tom),
                        medisinskArsak = null,
                        arbeidsrelatertArsak = null,
                    )
                }
                is OpprettSykmeldingAktivitet.Avventende -> {
                    SykmeldingRecordAktivitet.Avventende(
                        innspillTilArbeidsgiver =
                            payload.sykmelding.aktivitet.innspillTilArbeidsgiver,
                        fom = LocalDate.parse(payload.sykmelding.aktivitet.fom),
                        tom = LocalDate.parse(payload.sykmelding.aktivitet.tom),
                    )
                }
                is OpprettSykmeldingAktivitet.Behandlingsdager -> {
                    SykmeldingRecordAktivitet.Behandlingsdager(
                        antallBehandlingsdager =
                            payload.sykmelding.aktivitet.antallBehandlingsdager,
                        fom = LocalDate.parse(payload.sykmelding.aktivitet.fom),
                        tom = LocalDate.parse(payload.sykmelding.aktivitet.tom),
                    )
                }
                is OpprettSykmeldingAktivitet.Reisetilskudd -> {
                    SykmeldingRecordAktivitet.Reisetilskudd(
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
