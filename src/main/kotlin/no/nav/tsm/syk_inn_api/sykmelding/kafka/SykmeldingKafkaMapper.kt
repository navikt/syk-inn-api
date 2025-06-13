package no.nav.tsm.syk_inn_api.sykmelding.kafka

import java.time.LocalDate
import java.time.OffsetDateTime
import no.nav.tsm.regulus.regula.RegulaOutcomeStatus
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.Digital
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.HelsepersonellKategori
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.KafkaPersonNavn
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.MessageMetadata
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.PersonId
import no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata.PersonIdType
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.DigitalSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.DigitalSykmeldingMetadata
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.EnArbeidsgiver
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.FlereArbeidsgivere
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.IngenArbeidsgiver
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.KafkaDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.KafkaDiagnoseSystem
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.KafkaYrkesskade
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordArbeidsgiverInfo
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordBehandler
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordMedisinskVurdering
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordPasient
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecordTilbakedatering
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentArbeidsgiver
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeta
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentTilbakedatering
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentValues
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentYrkesskade
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
                    }
                }
            }

        val rules = listOf(rule)
        return ValidationResult(status = rule.type, timestamp = OffsetDateTime.now(), rules = rules)
    }

    fun mapMessageMetadata(meta: SykmeldingDocumentMeta): MessageMetadata =
        Digital(orgnummer = meta.legekontorOrgnr)

    fun mapToDigitalSykmelding(
        sykmelding: SykmeldingDocument,
        sykmeldingId: String,
        person: Person,
        sykmelder: HprSykmelder
    ): DigitalSykmelding {
        requireNotNull(sykmelder.fornavn)
        requireNotNull(sykmelder.etternavn)
        // TODO is it ok to use the first godkjenning?
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
                SykmeldingRecordPasient(
                    navn = person.navn,
                    fnr = sykmelding.meta.pasientIdent,
                    kontaktinfo = emptyList(),
                ),
            medisinskVurdering = mapMedisinskVurdering(sykmelding.values),
            aktivitet = sykmelding.values.aktivitet.map { toRecordAktivitet(it) },
            behandler =
                SykmeldingRecordBehandler(
                    navn =
                        KafkaPersonNavn(
                            fornavn = sykmelder.fornavn,
                            mellomnavn = sykmelder.mellomnavn,
                            etternavn = sykmelder.etternavn,
                        ),
                    ids = mapPersonIdsForSykmelder(sykmelder),
                    kontaktinfo = emptyList(),
                ),
            sykmelder =
                SykmeldingRecordSykmelder(
                    ids = mapPersonIdsForSykmelder(sykmelder),
                    helsepersonellKategori =
                        HelsepersonellKategori.parse(
                            helsepersonellKategoriKode.verdi,
                        ), // TODO er det rett verdi ??
                ),
            arbeidsgiver =
                mapArbeidsgiver(
                    sykmelding.values.arbeidsgiver,
                    sykmelding.values.meldinger,
                ),
            tilbakedatering = mapTilbakedatering(sykmelding.values.tilbakedatering),
            meldinger = mapMeldinger(sykmelding.values.meldinger),
        )
    }

    private fun mapMeldinger(meldinger: SykmeldingDocumentMeldinger): SykmeldingRecordMeldinger {
        return SykmeldingRecordMeldinger(
            tilNav = meldinger.tilNav,
            tilArbeidsgiver = meldinger.tilArbeidsgiver,
        )
    }

    private fun mapTilbakedatering(
        tilbakedatering: SykmeldingDocumentTilbakedatering?
    ): SykmeldingRecordTilbakedatering? {
        if (tilbakedatering == null) return null

        return SykmeldingRecordTilbakedatering(
            kontaktDato = tilbakedatering.startdato,
            begrunnelse = tilbakedatering.begrunnelse,
        )
    }

    private fun mapArbeidsgiver(
        arbeidsgiver: SykmeldingDocumentArbeidsgiver?,
        meldinger: SykmeldingDocumentMeldinger
    ): SykmeldingRecordArbeidsgiverInfo {
        if (arbeidsgiver == null) {
            return EnArbeidsgiver(
                meldingTilArbeidsgiver = meldinger.tilArbeidsgiver,
                tiltakArbeidsplassen = null,
            )
        }

        if (arbeidsgiver.harFlere) {
            return FlereArbeidsgivere(
                navn = arbeidsgiver.arbeidsgivernavn,
                yrkesbetegnelse = null,
                stillingsprosent = null,
                meldingTilArbeidsgiver = meldinger.tilArbeidsgiver,
                tiltakArbeidsplassen = null,
            )
        }
        return IngenArbeidsgiver()
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

    fun mapMedisinskVurdering(
        sykmeldingValues: SykmeldingDocumentValues
    ): SykmeldingRecordMedisinskVurdering {
        return SykmeldingRecordMedisinskVurdering(
            hovedDiagnose = mapHoveddiagnose(sykmeldingValues.hoveddiagnose),
            biDiagnoser = sykmeldingValues.bidiagnoser?.toSykmeldingRecordDiagnoseInfo(),
            svangerskap = sykmeldingValues.svangerskapsrelatert,
            skjermetForPasient = sykmeldingValues.pasientenSkalSkjermes,
            yrkesskade = sykmeldingValues.yrkesskade.toSykmeldingRecordYrkesskade(),
            syketilfelletStartDato = null,
            annenFraversArsak = null,
        )
    }

    fun toRecordAktivitet(aktivitet: SykmeldingDocumentAktivitet): SykmeldingRecordAktivitet {
        return when (aktivitet) {
            is SykmeldingDocumentAktivitet.Gradert -> {
                SykmeldingRecordAktivitet.Gradert(
                    grad = aktivitet.grad,
                    fom = LocalDate.parse(aktivitet.fom),
                    tom = LocalDate.parse(aktivitet.tom),
                    reisetilskudd = aktivitet.reisetilskudd,
                )
            }
            is SykmeldingDocumentAktivitet.IkkeMulig -> {
                SykmeldingRecordAktivitet.AktivitetIkkeMulig(
                    fom = LocalDate.parse(aktivitet.fom),
                    tom = LocalDate.parse(aktivitet.tom),
                    medisinskArsak = null,
                    arbeidsrelatertArsak = null,
                )
            }
            is SykmeldingDocumentAktivitet.Avventende -> {
                SykmeldingRecordAktivitet.Avventende(
                    innspillTilArbeidsgiver = aktivitet.innspillTilArbeidsgiver,
                    fom = LocalDate.parse(aktivitet.fom),
                    tom = LocalDate.parse(aktivitet.tom),
                )
            }
            is SykmeldingDocumentAktivitet.Behandlingsdager -> {
                SykmeldingRecordAktivitet.Behandlingsdager(
                    antallBehandlingsdager = aktivitet.antallBehandlingsdager,
                    fom = LocalDate.parse(aktivitet.fom),
                    tom = LocalDate.parse(aktivitet.tom),
                )
            }
            is SykmeldingDocumentAktivitet.Reisetilskudd -> {
                SykmeldingRecordAktivitet.Reisetilskudd(
                    fom = LocalDate.parse(aktivitet.fom),
                    tom = LocalDate.parse(aktivitet.tom),
                )
            }
        }
    }
}

fun mapHoveddiagnose(hoveddiagnose: SykmeldingDocumentDiagnoseInfo?): KafkaDiagnoseInfo? {
    if (hoveddiagnose == null) return null

    return KafkaDiagnoseInfo(
        system = hoveddiagnose.system.toKafkaDiagnoseSystem(),
        kode = hoveddiagnose.code,
    )
}

private fun DiagnoseSystem.toKafkaDiagnoseSystem(): KafkaDiagnoseSystem {
    return when (this) {
        DiagnoseSystem.ICPC2 -> KafkaDiagnoseSystem.ICPC2
        DiagnoseSystem.ICD10 -> KafkaDiagnoseSystem.ICD10
    }
}

private fun SykmeldingDocumentYrkesskade?.toSykmeldingRecordYrkesskade(): KafkaYrkesskade? {
    if (this == null) return null

    if (!this.yrkesskade) {
        return null
    }

    return KafkaYrkesskade(yrkesskadeDato = this.skadedato)
}

private fun List<SykmeldingDocumentDiagnoseInfo>.toSykmeldingRecordDiagnoseInfo():
    List<KafkaDiagnoseInfo>? {
    if (this.isEmpty()) {
        return null
    }

    return this.map { diagnose ->
        KafkaDiagnoseInfo(
            system = diagnose.system.toKafkaDiagnoseSystem(),
            kode = diagnose.code,
        )
    }
}
