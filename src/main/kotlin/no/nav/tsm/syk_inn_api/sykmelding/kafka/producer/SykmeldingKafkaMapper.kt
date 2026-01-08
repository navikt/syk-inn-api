package no.nav.tsm.syk_inn_api.sykmelding.kafka.producer

import java.time.OffsetDateTime
import no.nav.tsm.diagnoser.Diagnose
import no.nav.tsm.diagnoser.toICPC2
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.sykmelder.hpr.parseHelsepersonellKategori
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedInvalidRule
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedOKRule
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedPendingRule
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedReason
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedRuleType
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingArbeidsgiver
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingTilbakedatering
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingUtdypendeSporsmal
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingYrkesskade
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedValidationResult
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedValidationType
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingDb
import no.nav.tsm.syk_inn_api.sykmelding.response.SykInnArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.input.core.model.ArbeidsgiverInfo
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsak
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.AvsenderSystem
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
import no.nav.tsm.sykmelding.input.core.model.MedisinskArsak
import no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.OKRule
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.PendingRule
import no.nav.tsm.sykmelding.input.core.model.Reason
import no.nav.tsm.sykmelding.input.core.model.Reisetilskudd
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.Sporsmalstype
import no.nav.tsm.sykmelding.input.core.model.Tilbakedatering
import no.nav.tsm.sykmelding.input.core.model.UtdypendeSporsmal
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

    fun mapMessageMetadata(sykmelding: SykmeldingDb): MessageMetadata =
        Digital(
            orgnummer = sykmelding.legekontorOrgnr
                    ?: throw IllegalStateException(
                        "Unable to create sykmelding without legekontorOrgnr",
                    ),
        )

    fun mapToDigitalSykmelding(sykmelding: SykmeldingDb, source: String): DigitalSykmelding {
        val sykmelderNavn: Navn? =
            sykmelding.sykmelding.sykmelder.let {
                if (it.fornavn == null || it.etternavn == null) {
                    null
                } else {
                    Navn(
                        fornavn = it.fornavn,
                        mellomnavn = it.mellomnavn,
                        etternavn = it.etternavn,
                    )
                }
            }

        requireNotNull(sykmelderNavn) { "Sykmelder must have a name" }

        // TODO is it ok to use the first godkjenning? NO need to find the best one first
        val helsepersonellKategoriKode =
            sykmelding.sykmelding.sykmelder.godkjenninger.first().helsepersonellkategori
        requireNotNull(helsepersonellKategoriKode)

        return DigitalSykmelding(
            id = sykmelding.sykmeldingId,
            metadata =
                DigitalSykmeldingMetadata(
                    mottattDato = OffsetDateTime.now(),
                    genDate = OffsetDateTime.now(),
                    avsenderSystem = AvsenderSystem(source, "1"),
                ),
            pasient =
                Pasient(
                    navn =
                        Navn(
                            fornavn = sykmelding.sykmelding.pasient.navn.fornavn,
                            mellomnavn = sykmelding.sykmelding.pasient.navn.mellomnavn,
                            etternavn = sykmelding.sykmelding.pasient.navn.etternavn,
                        ),
                    navKontor = null,
                    navnFastlege = null,
                    fnr = sykmelding.sykmelding.pasient.ident,
                    kontaktinfo = emptyList(),
                ),
            medisinskVurdering = mapMedisinskVurdering(sykmelding.sykmelding),
            aktivitet = sykmelding.sykmelding.aktivitet.map { toRecordAktivitet(it) },
            behandler =
                Behandler(
                    navn =
                        Navn(
                            fornavn = sykmelderNavn.fornavn,
                            mellomnavn = sykmelderNavn.mellomnavn,
                            etternavn = sykmelderNavn.etternavn,
                        ),
                    ids = mapPersonIdsForSykmelder(sykmelding.sykmelding.sykmelder),
                    kontaktinfo =
                        if (sykmelding.legekontorTlf != null)
                            listOf(
                                Kontaktinfo(
                                    type = KontaktinfoType.TLF,
                                    value = sykmelding.legekontorTlf,
                                ),
                            )
                        else emptyList(),
                    adresse = null,
                ),
            sykmelder =
                no.nav.tsm.sykmelding.input.core.model.Sykmelder(
                    ids = mapPersonIdsForSykmelder(sykmelding.sykmelding.sykmelder),
                    helsepersonellKategori =
                        parseHelsepersonellKategori(helsepersonellKategoriKode.verdi),
                ),
            arbeidsgiver =
                mapArbeidsgiver(
                    sykmelding.sykmelding.arbeidsgiver,
                    sykmelding.sykmelding.meldinger,
                ),
            tilbakedatering = mapTilbakedatering(sykmelding.sykmelding.tilbakedatering),
            utdypendeSporsmal = mapUtdypendeSporsmal(sykmelding.sykmelding.utdypendeSporsmal),
            bistandNav = mapBistandNav(sykmelding.sykmelding.meldinger),
        )
    }

    private fun mapBistandNav(meldinger: PersistedSykmeldingMeldinger): BistandNav {
        return BistandNav(bistandUmiddelbart = false, beskrivBistand = meldinger.tilNav)
    }

    private fun mapTilbakedatering(
        tilbakedatering: PersistedSykmeldingTilbakedatering?
    ): Tilbakedatering? {
        if (tilbakedatering == null) return null

        return Tilbakedatering(
            kontaktDato = tilbakedatering.startdato,
            begrunnelse = tilbakedatering.begrunnelse,
        )
    }

    private fun mapUtdypendeSporsmal(
        utdypendeSporsmal: PersistedSykmeldingUtdypendeSporsmal?
    ): List<UtdypendeSporsmal>? {
        if (utdypendeSporsmal == null) {
            return null
        }
        val utdypendeSporsmalList = mutableListOf<UtdypendeSporsmal>()
        if (utdypendeSporsmal.utfordringerMedArbeid != null) {
            utdypendeSporsmalList.add(
                UtdypendeSporsmal(
                    utdypendeSporsmal.utfordringerMedArbeid,
                    Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID,
                ),
            )
        }
        if (utdypendeSporsmal.medisinskOppsummering != null) {
            utdypendeSporsmalList.add(
                UtdypendeSporsmal(
                    utdypendeSporsmal.medisinskOppsummering,
                    Sporsmalstype.MEDISINSK_OPPSUMMERING,
                ),
            )
        }
        if (utdypendeSporsmal.hensynPaArbeidsplassen != null) {
            utdypendeSporsmalList.add(
                UtdypendeSporsmal(
                    utdypendeSporsmal.hensynPaArbeidsplassen,
                    Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN,
                ),
            )
        }
        return utdypendeSporsmalList
    }

    private fun mapArbeidsgiver(
        arbeidsgiver: PersistedSykmeldingArbeidsgiver?,
        meldinger: PersistedSykmeldingMeldinger,
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

    private fun mapPersonIdsForSykmelder(sykmelder: PersistedSykmeldingSykmelder): List<PersonId> {
        return listOf(
            PersonId(
                id = sykmelder.hprNummer,
                type = PersonIdType.HPR,
            ),
            PersonId(
                id = sykmelder.ident,
                type = PersonIdType.FNR,
            ),
        )
    }

    fun mapMedisinskVurdering(
        sykmelding: PersistedSykmelding,
    ): MedisinskVurdering {
        return MedisinskVurdering(
            hovedDiagnose = sykmelding.hoveddiagnose?.toDiagnoseInfo(),
            biDiagnoser = sykmelding.bidiagnoser.toSykmeldingRecordDiagnoseInfo(),
            svangerskap = sykmelding.svangerskapsrelatert,
            skjermetForPasient = sykmelding.pasientenSkalSkjermes,
            yrkesskade = sykmelding.yrkesskade.toSykmeldingRecordYrkesskade(),
            syketilfelletStartDato = null,
            annenFraversArsak = null,
        )
    }

    fun toRecordAktivitet(aktivitet: PersistedSykmeldingAktivitet): Aktivitet {
        return when (aktivitet) {
            is PersistedSykmeldingAktivitet.Gradert -> {
                Gradert(
                    grad = aktivitet.grad,
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                    reisetilskudd = aktivitet.reisetilskudd,
                )
            }
            is PersistedSykmeldingAktivitet.IkkeMulig -> {
                AktivitetIkkeMulig(
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                    medisinskArsak =
                        if (aktivitet.medisinskArsak.isMedisinskArsak)
                            MedisinskArsak(
                                arsak = listOf(),
                                beskrivelse = null,
                            )
                        else null,
                    arbeidsrelatertArsak =
                        if (aktivitet.arbeidsrelatertArsak.isArbeidsrelatertArsak)
                            ArbeidsrelatertArsak(
                                arsak =
                                    aktivitet.arbeidsrelatertArsak.arbeidsrelaterteArsaker.map {
                                        when (it) {
                                            SykInnArbeidsrelatertArsakType
                                                .TILRETTELEGGING_IKKE_MULIG ->
                                                ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING
                                            SykInnArbeidsrelatertArsakType.ANNET ->
                                                ArbeidsrelatertArsakType.ANNET
                                        }
                                    },
                                beskrivelse =
                                    aktivitet.arbeidsrelatertArsak.annenArbeidsrelatertArsak,
                            )
                        else null,
                )
            }
            is PersistedSykmeldingAktivitet.Avventende -> {
                Avventende(
                    innspillTilArbeidsgiver = aktivitet.innspillTilArbeidsgiver,
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                )
            }
            is PersistedSykmeldingAktivitet.Behandlingsdager -> {
                Behandlingsdager(
                    antallBehandlingsdager = aktivitet.antallBehandlingsdager,
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                )
            }
            is PersistedSykmeldingAktivitet.Reisetilskudd -> {
                Reisetilskudd(
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                )
            }
        }
    }

    fun mapValidationResult(persisted: PersistedValidationResult): ValidationResult {
        return ValidationResult(
            status =
                when (persisted.status) {
                    PersistedRuleType.OK -> RuleType.OK
                    PersistedRuleType.PENDING -> RuleType.PENDING
                    PersistedRuleType.INVALID -> RuleType.INVALID
                },
            timestamp = persisted.timestamp,
            rules =
                persisted.rules.map { persistedRule ->
                    when (persistedRule) {
                        is PersistedOKRule ->
                            OKRule(
                                name = persistedRule.name,
                                timestamp = persistedRule.timestamp,
                                validationType = toValidationType(persistedRule.validationType),
                            )
                        is PersistedInvalidRule ->
                            InvalidRule(
                                name = persistedRule.name,
                                timestamp = persistedRule.timestamp,
                                validationType = toValidationType(persistedRule.validationType),
                                reason = toReason(persistedRule.reason),
                            )
                        is PersistedPendingRule ->
                            PendingRule(
                                name = persistedRule.name,
                                timestamp = persistedRule.timestamp,
                                validationType = toValidationType(persistedRule.validationType),
                                reason = toReason(persistedRule.reason),
                            )
                    }
                },
        )
    }

    private fun toReason(reason: PersistedReason): Reason =
        Reason(
            sykmeldt = reason.sykmeldt,
            sykmelder = reason.sykmelder,
        )

    private fun toValidationType(validationType: PersistedValidationType): ValidationType =
        when (validationType) {
            PersistedValidationType.AUTOMATIC -> ValidationType.AUTOMATIC
            PersistedValidationType.MANUAL -> ValidationType.MANUAL
        }
}

fun mapHoveddiagnose(hoveddiagnose: PersistedSykmeldingDiagnoseInfo?): DiagnoseInfo? {
    if (hoveddiagnose == null) return null

    return DiagnoseInfo(
        system = hoveddiagnose.system.toKafkaDiagnoseSystem(),
        kode = hoveddiagnose.code,
        tekst = hoveddiagnose.text,
    )
}

private fun DiagnoseSystem.toKafkaDiagnoseSystem():
    no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem {
    return when (this) {
        DiagnoseSystem.ICPC2 -> no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem.ICPC2
        DiagnoseSystem.ICD10 -> no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem.ICD10
        DiagnoseSystem.ICPC2B -> no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem.ICPC2B
        DiagnoseSystem.PHBU -> no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem.PHBU
        DiagnoseSystem.UGYLDIG -> no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem.UGYLDIG
    }
}

private fun PersistedSykmeldingYrkesskade?.toSykmeldingRecordYrkesskade(): Yrkesskade? {
    if (this == null) return null

    if (!this.yrkesskade) {
        return null
    }

    return Yrkesskade(yrkesskadeDato = this.skadedato)
}

private fun List<PersistedSykmeldingDiagnoseInfo>.toSykmeldingRecordDiagnoseInfo():
    List<DiagnoseInfo>? {
    if (this.isEmpty()) {
        return null
    }

    return this.map { diagnose -> diagnose.toDiagnoseInfo() }
}

private fun PersistedSykmeldingDiagnoseInfo.toDiagnoseInfo(): DiagnoseInfo {
    if (this.system == DiagnoseSystem.ICPC2B) {
        return Diagnose.from(system.name, code)?.toICPC2()?.let {
            DiagnoseInfo(
                system = DiagnoseSystem.ICPC2.toKafkaDiagnoseSystem(),
                kode = it.code,
                tekst = it.text,
            )
        }
            ?: throw IllegalStateException("ICPC2B code $code could not be mapped to ICPC2")
    }

    return DiagnoseInfo(
        system = system.toKafkaDiagnoseSystem(),
        kode = code,
        tekst = text,
    )
}
