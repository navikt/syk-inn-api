package no.nav.tsm.syk_inn_api.sykmelding.kafka.producer

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import no.nav.tsm.regulus.regula.RegulaOutcomeStatus
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.common.DiagnosekodeMapper.findTextFromDiagnoseSystem
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelder.hpr.parseHelsepersonellKategori
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingArbeidsgiver
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingMetadata
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingPayload
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingTilbakedatering
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingYrkesskade
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.input.core.model.ArbeidsgiverInfo
import no.nav.tsm.sykmelding.input.core.model.Avventende
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.Behandlingsdager
import no.nav.tsm.sykmelding.input.core.model.BistandNav
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmeldingMetadata
import no.nav.tsm.sykmelding.input.core.model.EnArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.FlereArbeidsgivere
import no.nav.tsm.sykmelding.input.core.model.Gradert
import no.nav.tsm.sykmelding.input.core.model.IngenArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.InvalidRule
import no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.PendingRule
import no.nav.tsm.sykmelding.input.core.model.Reason
import no.nav.tsm.sykmelding.input.core.model.Reisetilskudd
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.Tilbakedatering
import no.nav.tsm.sykmelding.input.core.model.TilbakedatertMerknad
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.ValidationType
import no.nav.tsm.sykmelding.input.core.model.Yrkesskade
import no.nav.tsm.sykmelding.input.core.model.metadata.Digital
import no.nav.tsm.sykmelding.input.core.model.metadata.Kontaktinfo
import no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import no.nav.tsm.sykmelding.input.core.model.metadata.Navn
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonId
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType

object SykmeldingKafkaMapper {
    fun mapValidationResult(regulaResult: RegulaResult): ValidationResult {
        val ruleTimestamp = OffsetDateTime.now(ZoneOffset.UTC)
        val status =
            when (regulaResult.status) {
                RegulaStatus.OK -> RuleType.OK
                RegulaStatus.INVALID -> RuleType.INVALID
                RegulaStatus.MANUAL_PROCESSING -> RuleType.PENDING
            }
        val validation =
            when (regulaResult) {
                is RegulaResult.Ok -> ValidationResult(RuleType.OK, ruleTimestamp, emptyList())
                is RegulaResult.NotOk -> {
                    val name =
                        when {
                            isManualTilbakedatering(regulaResult) ->
                                TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name
                            else -> regulaResult.outcome.rule
                        }
                    val validationType = ValidationType.AUTOMATIC
                    val reason =
                        Reason(
                            sykmeldt = regulaResult.outcome.reason.sykmeldt,
                            sykmelder = regulaResult.outcome.reason.sykmelder,
                        )

                    val rule =
                        when (regulaResult.outcome.status) {
                            RegulaOutcomeStatus.INVALID ->
                                InvalidRule(name, validationType, ruleTimestamp, reason)
                            RegulaOutcomeStatus.MANUAL_PROCESSING ->
                                PendingRule(name, ruleTimestamp, validationType, reason)
                        }
                    ValidationResult(status, ruleTimestamp, rules = listOf(rule))
                }
            }
        return validation
    }

    private fun isManualTilbakedatering(regulaResult: RegulaResult.NotOk): Boolean =
        regulaResult.outcome.status == RegulaOutcomeStatus.MANUAL_PROCESSING &&
            regulaResult.outcome.tree == "Tilbakedatering"

    fun mapMessageMetadata(meta: OpprettSykmeldingMetadata): MessageMetadata =
        Digital(orgnummer = meta.legekontorOrgnr)

    fun mapToDigitalSykmelding(
        sykmelding: OpprettSykmeldingPayload,
        sykmeldingId: String,
        person: Person,
        sykmelder: Sykmelder
    ): DigitalSykmelding {
        val sykmelderNavn: Navn? =
            sykmelder.navn?.let {
                Navn(
                    fornavn = it.fornavn,
                    mellomnavn = it.mellomnavn,
                    etternavn = it.etternavn,
                )
            }

        requireNotNull(sykmelderNavn) { "Sykmelder must have a name" }

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
                Pasient(
                    navn =
                        Navn(
                            fornavn = person.navn.fornavn,
                            mellomnavn = person.navn.mellomnavn,
                            etternavn = person.navn.etternavn,
                        ),
                    navKontor = null,
                    navnFastlege = null,
                    fnr = person.ident,
                    kontaktinfo = emptyList()
                ),
            medisinskVurdering = mapMedisinskVurdering(sykmelding.values),
            aktivitet = sykmelding.values.aktivitet.map { toRecordAktivitet(it) },
            behandler =
                Behandler(
                    navn =
                        Navn(
                            fornavn = sykmelderNavn.fornavn,
                            mellomnavn = sykmelderNavn.mellomnavn,
                            etternavn = sykmelderNavn.etternavn,
                        ),
                    ids = mapPersonIdsForSykmelder(sykmelder),
                    kontaktinfo =
                        listOf(
                            Kontaktinfo(
                                type = KontaktinfoType.TLF,
                                value = sykmelding.meta.legekontorTlf,
                            )
                        ),
                    adresse = null,
                ),
            sykmelder =
                no.nav.tsm.sykmelding.input.core.model.Sykmelder(
                    ids = mapPersonIdsForSykmelder(sykmelder),
                    helsepersonellKategori =
                        parseHelsepersonellKategori(helsepersonellKategoriKode.verdi),
                ),
            arbeidsgiver =
                mapArbeidsgiver(
                    sykmelding.values.arbeidsgiver,
                    sykmelding.values.meldinger,
                ),
            tilbakedatering = mapTilbakedatering(sykmelding.values.tilbakedatering),
            bistandNav = mapBistandNav(sykmelding.values.meldinger),
        )
    }

    private fun mapBistandNav(meldinger: OpprettSykmeldingMeldinger): BistandNav? {
        return BistandNav(bistandUmiddelbart = false, beskrivBistand = meldinger.tilNav)
    }

    private fun mapTilbakedatering(
        tilbakedatering: OpprettSykmeldingTilbakedatering?
    ): Tilbakedatering? {
        if (tilbakedatering == null) return null

        return Tilbakedatering(
            kontaktDato = tilbakedatering.startdato,
            begrunnelse = tilbakedatering.begrunnelse,
        )
    }

    private fun mapArbeidsgiver(
        arbeidsgiver: OpprettSykmeldingArbeidsgiver?,
        meldinger: OpprettSykmeldingMeldinger
    ): ArbeidsgiverInfo {
        if (arbeidsgiver == null) {
            return EnArbeidsgiver(
                meldingTilArbeidsgiver = meldinger.tilArbeidsgiver,
                tiltakArbeidsplassen = null,
                navn = null,
                yrkesbetegnelse = null,
                stillingsprosent = null,
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

    private fun mapPersonIdsForSykmelder(sykmelder: Sykmelder): List<PersonId> {
        return listOf(
            PersonId(
                id = sykmelder.hpr,
                type = PersonIdType.HPR,
            ),
            PersonId(
                id = sykmelder.ident,
                type = PersonIdType.FNR,
            ),
        )
    }

    fun mapMedisinskVurdering(
        sykmeldingValues: OpprettSykmelding,
    ): MedisinskVurdering {
        return MedisinskVurdering(
            hovedDiagnose = mapHoveddiagnose(sykmeldingValues.hoveddiagnose),
            biDiagnoser = sykmeldingValues.bidiagnoser?.toSykmeldingRecordDiagnoseInfo(),
            svangerskap = sykmeldingValues.svangerskapsrelatert,
            skjermetForPasient = sykmeldingValues.pasientenSkalSkjermes,
            yrkesskade = sykmeldingValues.yrkesskade.toSykmeldingRecordYrkesskade(),
            syketilfelletStartDato = null,
            annenFraversArsak = null,
        )
    }

    fun toRecordAktivitet(aktivitet: OpprettSykmeldingAktivitet): Aktivitet {
        return when (aktivitet) {
            is OpprettSykmeldingAktivitet.Gradert -> {
                Gradert(
                    grad = aktivitet.grad,
                    fom = LocalDate.parse(aktivitet.fom),
                    tom = LocalDate.parse(aktivitet.tom),
                    reisetilskudd = aktivitet.reisetilskudd,
                )
            }
            is OpprettSykmeldingAktivitet.IkkeMulig -> {
                AktivitetIkkeMulig(
                    fom = LocalDate.parse(aktivitet.fom),
                    tom = LocalDate.parse(aktivitet.tom),
                    medisinskArsak = null,
                    arbeidsrelatertArsak = null,
                )
            }
            is OpprettSykmeldingAktivitet.Avventende -> {
                Avventende(
                    innspillTilArbeidsgiver = aktivitet.innspillTilArbeidsgiver,
                    fom = LocalDate.parse(aktivitet.fom),
                    tom = LocalDate.parse(aktivitet.tom),
                )
            }
            is OpprettSykmeldingAktivitet.Behandlingsdager -> {
                Behandlingsdager(
                    antallBehandlingsdager = aktivitet.antallBehandlingsdager,
                    fom = LocalDate.parse(aktivitet.fom),
                    tom = LocalDate.parse(aktivitet.tom),
                )
            }
            is OpprettSykmeldingAktivitet.Reisetilskudd -> {
                Reisetilskudd(
                    fom = LocalDate.parse(aktivitet.fom),
                    tom = LocalDate.parse(aktivitet.tom),
                )
            }
        }
    }
}

fun mapHoveddiagnose(hoveddiagnose: OpprettSykmeldingDiagnoseInfo): DiagnoseInfo? {
    if (hoveddiagnose == null) return null

    return DiagnoseInfo(
        system = hoveddiagnose.system.toKafkaDiagnoseSystem(),
        kode = hoveddiagnose.code,
        tekst =
            findTextFromDiagnoseSystem(system = hoveddiagnose.system, code = hoveddiagnose.code),
    )
}

private fun DiagnoseSystem.toKafkaDiagnoseSystem():
    no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem {
    return when (this) {
        DiagnoseSystem.ICPC2 -> no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem.ICPC2
        DiagnoseSystem.ICD10 -> no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem.ICD10
    }
}

private fun OpprettSykmeldingYrkesskade?.toSykmeldingRecordYrkesskade(): Yrkesskade? {
    if (this == null) return null

    if (!this.yrkesskade) {
        return null
    }

    return Yrkesskade(yrkesskadeDato = this.skadedato)
}

private fun List<OpprettSykmeldingDiagnoseInfo>.toSykmeldingRecordDiagnoseInfo():
    List<DiagnoseInfo>? {
    if (this.isEmpty()) {
        return null
    }

    return this.map { diagnose ->
        DiagnoseInfo(
            system = diagnose.system.toKafkaDiagnoseSystem(),
            kode = diagnose.code,
            tekst = findTextFromDiagnoseSystem(system = diagnose.system, code = diagnose.code)
        )
    }
}
