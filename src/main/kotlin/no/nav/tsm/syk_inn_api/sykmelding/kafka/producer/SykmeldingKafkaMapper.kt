package no.nav.tsm.syk_inn_api.sykmelding.kafka.producer

import java.time.OffsetDateTime
import java.time.ZoneOffset
import no.nav.tsm.regulus.regula.RegulaOutcomeStatus
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelder.hpr.parseHelsepersonellKategori
import no.nav.tsm.syk_inn_api.sykmelding.response.SykInnArbeidsrelatertArsakType
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentArbeidsgiver
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeta
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentTilbakedatering
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentUtdypendeSporsmal
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentValues
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentYrkesskade
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
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.PendingRule
import no.nav.tsm.sykmelding.input.core.model.Reason
import no.nav.tsm.sykmelding.input.core.model.Reisetilskudd
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.Sporsmalstype
import no.nav.tsm.sykmelding.input.core.model.Tilbakedatering
import no.nav.tsm.sykmelding.input.core.model.TilbakedatertMerknad
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

    fun mapMessageMetadata(meta: SykmeldingDocumentMeta): MessageMetadata =
        Digital(
            orgnummer = meta.legekontorOrgnr
                    ?: throw IllegalStateException(
                        "Unable to create sykmelding without legekontorOrgnr"
                    )
        )

    fun mapToDigitalSykmelding(
        sykmelding: SykmeldingDocument,
        sykmeldingId: String,
        person: Person,
        sykmelder: Sykmelder,
        source: String
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

        // TODO is it ok to use the first godkjenning? NO need to find the best one first
        val helsepersonellKategoriKode = sykmelder.godkjenninger.first().helsepersonellkategori
        requireNotNull(helsepersonellKategoriKode)

        return DigitalSykmelding(
            id = sykmeldingId,
            metadata =
                DigitalSykmeldingMetadata(
                    mottattDato = OffsetDateTime.now(),
                    genDate = OffsetDateTime.now(),
                    avsenderSystem = AvsenderSystem(source, "1")
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
                    kontaktinfo = emptyList(),
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
                        if (sykmelding.meta.legekontorTlf != null)
                            listOf(
                                Kontaktinfo(
                                    type = KontaktinfoType.TLF,
                                    value = sykmelding.meta.legekontorTlf,
                                ),
                            )
                        else emptyList(),
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
            utdypendeSporsmal = mapUtdypendeSporsmal(sykmelding.values.utdypendeSporsmal),
            bistandNav = mapBistandNav(sykmelding.values.meldinger),
        )
    }

    private fun mapBistandNav(meldinger: SykmeldingDocumentMeldinger): BistandNav? {
        return BistandNav(bistandUmiddelbart = false, beskrivBistand = meldinger.tilNav)
    }

    private fun mapTilbakedatering(
        tilbakedatering: SykmeldingDocumentTilbakedatering?
    ): Tilbakedatering? {
        if (tilbakedatering == null) return null

        return Tilbakedatering(
            kontaktDato = tilbakedatering.startdato,
            begrunnelse = tilbakedatering.begrunnelse,
        )
    }

    private fun mapUtdypendeSporsmal(
        utdypendeSporsmal: SykmeldingDocumentUtdypendeSporsmal?
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
        arbeidsgiver: SykmeldingDocumentArbeidsgiver?,
        meldinger: SykmeldingDocumentMeldinger
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
        sykmeldingValues: SykmeldingDocumentValues,
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

    fun toRecordAktivitet(aktivitet: SykmeldingDocumentAktivitet): Aktivitet {
        return when (aktivitet) {
            is SykmeldingDocumentAktivitet.Gradert -> {
                Gradert(
                    grad = aktivitet.grad,
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                    reisetilskudd = aktivitet.reisetilskudd,
                )
            }
            is SykmeldingDocumentAktivitet.IkkeMulig -> {
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
            is SykmeldingDocumentAktivitet.Avventende -> {
                Avventende(
                    innspillTilArbeidsgiver = aktivitet.innspillTilArbeidsgiver,
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                )
            }
            is SykmeldingDocumentAktivitet.Behandlingsdager -> {
                Behandlingsdager(
                    antallBehandlingsdager = aktivitet.antallBehandlingsdager,
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                )
            }
            is SykmeldingDocumentAktivitet.Reisetilskudd -> {
                Reisetilskudd(
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                )
            }
        }
    }
}

fun mapHoveddiagnose(hoveddiagnose: SykmeldingDocumentDiagnoseInfo?): DiagnoseInfo? {
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

private fun SykmeldingDocumentYrkesskade?.toSykmeldingRecordYrkesskade(): Yrkesskade? {
    if (this == null) return null

    if (!this.yrkesskade) {
        return null
    }

    return Yrkesskade(yrkesskadeDato = this.skadedato)
}

private fun List<SykmeldingDocumentDiagnoseInfo>.toSykmeldingRecordDiagnoseInfo():
    List<DiagnoseInfo>? {
    if (this.isEmpty()) {
        return null
    }

    return this.map { diagnose ->
        DiagnoseInfo(
            system = diagnose.system.toKafkaDiagnoseSystem(),
            kode = diagnose.code,
            tekst = diagnose.text,
        )
    }
}
