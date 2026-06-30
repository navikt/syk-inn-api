package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce

import java.time.OffsetDateTime
import no.nav.tsm.core.common.SykInnDiagnoseSystem
import no.nav.tsm.modules.sykmeldinger.domain.*
import no.nav.tsm.sykmelding.input.core.model.*
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.metadata.*

fun VerifiedSykInnSykmelding.toInputRecord(
    /**
     * Because the kafka record requires the "old" regulus reason texts, they are in between user
     * saving and being published to kafka stored in the sykmelding_status table, as no other part
     * of the data flow actually needs these texts.
     */
    reason: Reason?
): SykmeldingRecord {
    val meta =
        when (this.meta) {
            is SykInnSykmeldingMeta.Digital -> this.meta
            else ->
                throw IllegalStateException(
                    "Should never map not digital sykmelding to SykmeldingRecord, was type: ${this.type} (ID: ${this.sykmeldingId})"
                )
        }
    return SykmeldingRecord.Digital(
        metadata = MessageMetadata.Digital(orgnummer = meta.legekontorOrgnr),
        validation = result.toValidationResult(meta.mottatt, reason),
        sykmelding =
            Sykmelding.Digital(
                id = sykmeldingId.toString(),
                metadata =
                    SykmeldingMeta.Digital(
                        mottattDato = meta.mottatt,
                        genDate = meta.mottatt,
                        avsenderSystem = AvsenderSystem(meta.source, "1"),
                    ),
                pasient =
                    Pasient(
                        fnr = meta.pasient.ident,
                        navn =
                            Navn(
                                // TODO() how to handle nullable names?
                                fornavn =
                                    meta.pasient.fornavn
                                        ?: throw IllegalStateException(
                                            "Fornavn should not be null"
                                        ),
                                mellomnavn = meta.pasient.mellomnavn,
                                etternavn =
                                    meta.pasient.etternavn
                                        ?: throw IllegalStateException(
                                            "Etternavn should not be null"
                                        ),
                            ),
                        navKontor = null,
                        navnFastlege = null,
                        kontaktinfo = emptyList(),
                    ),
                medisinskVurdering =
                    MedisinskVurdering.Digital(
                        hovedDiagnose = values.hoveddiagnose?.toDiagnoseInfo(),
                        biDiagnoser = values.bidiagnoser.map { it.toDiagnoseInfo() },
                        svangerskap = values.svangerskapsrelatert,
                        skjermetForPasient = values.pasientenSkalSkjermes,
                        yrkesskade = values.yrkesskade?.let { Yrkesskade(it.skadedato) },
                        annenFravarsgrunn = values.annenFravarsgrunn,
                    ),
                aktivitet = values.aktivitet.map { it.toAktivitet() },
                behandler =
                    Behandler(
                        navn =
                            Navn(
                                fornavn =
                                    meta.behandler.fornavn
                                        ?: throw IllegalStateException(
                                            "Fornavn should not be null"
                                        ),
                                mellomnavn = meta.behandler.mellomnavn,
                                etternavn =
                                    meta.behandler.etternavn
                                        ?: throw IllegalStateException(
                                            "Etternavn should not be null"
                                        ),
                            ),
                        adresse = null,
                        ids =
                            listOf(
                                PersonId(type = PersonIdType.HPR, id = meta.behandler.hpr),
                                PersonId(type = PersonIdType.FNR, id = meta.behandler.ident),
                            ),
                        kontaktinfo =
                            listOf(
                                Kontaktinfo(type = KontaktinfoType.TLF, value = meta.legekontorTlf)
                            ),
                    ),
                sykmelder =
                    Sykmelder(
                        helsepersonellKategori =
                            meta.behandler.helsepersonellkategori.toHelsepersonellkategori(),
                        ids = listOf(PersonId(type = PersonIdType.HPR, id = meta.behandler.hpr)),
                    ),
                arbeidsgiver =
                    values.arbeidsgiver.toArbeidsgiver(values.meldinger?.tilArbeidsgiver),
                tilbakedatering = values.tilbakedatering?.toTilbakedatering(),
                bistandNav = values.meldinger?.toBistandNav(),
                utdypendeSporsmal = values.utdypendeSporsmal?.toUtdypendeOpplysninger(),
            ),
    )
}

private fun SykInnSykmeldingRuleResult.toValidationResult(
    mottatt: OffsetDateTime,
    reason: Reason?,
): ValidationResult =
    when (this) {
        is SykInnSykmeldingRuleResult.OK -> ValidationResult(RuleType.OK, mottatt, emptyList())
        is SykInnSykmeldingRuleResult.Outcome -> {
            val rule =
                when (type) {
                    RuleType.OK -> throw IllegalStateException("Rule with outcome can't be OK")
                    RuleType.PENDING ->
                        Rule.Pending(
                            rule,
                            mottatt,
                            ValidationType.AUTOMATIC,
                            reason
                                ?: throw IllegalStateException(
                                    "Rule with outcome 'PENDING' can't be without reason"
                                ),
                        )

                    RuleType.INVALID ->
                        Rule.Invalid(
                            rule,
                            ValidationType.AUTOMATIC,
                            mottatt,
                            reason
                                ?: throw IllegalStateException(
                                    "Rule with outcome 'INVALID' can't be without reason"
                                ),
                        )
                }

            ValidationResult(type, mottatt, listOf(rule))
        }
    }

private fun SykInnDiagnoseInfo.toDiagnoseInfo(): DiagnoseInfo =
    DiagnoseInfo(
        system =
            when (this) {
                is SykInnDiagnoseInfo.Valid ->
                    when (system) {
                        SykInnDiagnoseSystem.ICPC2 -> DiagnoseSystem.ICPC2
                        SykInnDiagnoseSystem.ICD10 -> DiagnoseSystem.ICD10
                        SykInnDiagnoseSystem.ICPC2B -> DiagnoseSystem.ICPC2B
                    }
                is SykInnDiagnoseInfo.Invalid ->
                    error(
                        "A DIGITAL sykmelding should never produce a sykmelding with any non-supported DiagnoseSystem"
                    )
            },
        kode = code,
        tekst = maybeTekst,
    )

private fun SykInnTilbakedatering.toTilbakedatering(): Tilbakedatering =
    Tilbakedatering(kontaktDato = kontaktdato, begrunnelse = begrunnelse)

private fun SykInnMeldinger.toBistandNav(): BistandNav? = tilNav?.let {
    BistandNav(bistandUmiddelbart = false, beskrivBistand = it)
}

private fun List<String>.toHelsepersonellkategori(): HelsepersonellKategori =
    map { parseHelsepersonellKategori(it) }.minBy { helsepersonellkategoriPresedence(it) }

private fun SykInnAktivitet.toAktivitet(): Aktivitet =
    when (this) {
        is SykInnAktivitet.IkkeMulig ->
            Aktivitet.IkkeMulig(
                fom = fom,
                tom = tom,
                arbeidsrelatertArsak =
                    arbeidsrelatertArsak?.let {
                        ArbeidsrelatertArsak(
                            beskrivelse = arbeidsrelatertArsak.annenArbeidsrelatertArsak,
                            arsak = arbeidsrelatertArsak.arbeidsrelaterteArsaker,
                        )
                    },
                // medisinskArsak er soft deprekert, settes til null med vilje
                medisinskArsak = null,
            )

        is SykInnAktivitet.Gradert ->
            Aktivitet.Gradert(fom = fom, tom = tom, grad = grad, reisetilskudd = reisetilskudd)

        is SykInnAktivitet.Avventende ->
            Aktivitet.Avventende(
                fom = fom,
                tom = tom,
                innspillTilArbeidsgiver = innspillTilArbeidsgiver,
            )

        is SykInnAktivitet.Behandlingsdager ->
            Aktivitet.Behandlingsdager(
                fom = fom,
                tom = tom,
                antallBehandlingsdager = antallBehandlingsdager,
            )

        is SykInnAktivitet.Reisetilskudd -> Aktivitet.Reisetilskudd(fom = fom, tom = tom)
    }

private fun SykInnArbeidsgiver?.toArbeidsgiver(meldingTilArbeidsgiver: String?): ArbeidsgiverInfo {
    if (this == null && meldingTilArbeidsgiver == null) {
        return ArbeidsgiverInfo.Ingen()
    }

    return when {
        // User explicitly has multiple
        this != null && this.harFlere ->
            ArbeidsgiverInfo.Flere(
                navn = this.arbeidsgivernavn,
                meldingTilArbeidsgiver = meldingTilArbeidsgiver,
                // syk-inn-api har ikke noe med disse verdiene å gjøre
                yrkesbetegnelse = null,
                stillingsprosent = null,
                tiltakArbeidsplassen = null,
            )
        // User does _not_ have multiple, but has provided a message to the employer, assume one
        else ->
            ArbeidsgiverInfo.En(
                meldingTilArbeidsgiver = meldingTilArbeidsgiver,
                // syk-inn-api har ikke noe med disse verdiene å gjøre
                navn = null,
                yrkesbetegnelse = null,
                stillingsprosent = null,
                tiltakArbeidsplassen = null,
            )
    }
}

private fun SykInnUtdypendeSporsmal.toUtdypendeOpplysninger(): List<UtdypendeSporsmal>? {
    // TODO: Sanity check mapping between these enums and distinct values
    return listOfNotNull(
            hensynPaArbeidsplassen?.toUtdypendeSporsmal(Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN),
            medisinskOppsummering?.toUtdypendeSporsmal(Sporsmalstype.MEDISINSK_OPPSUMMERING),
            utfordringerMedGradertArbeid?.toUtdypendeSporsmal(
                Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID
            ),
            utfordringerMedArbeid?.toUtdypendeSporsmal(Sporsmalstype.UTFORDRINGER_MED_ARBEID),
            behandlingOgFremtidigArbeid?.toUtdypendeSporsmal(
                Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID
            ),
            uavklarteForhold?.toUtdypendeSporsmal(Sporsmalstype.UAVKLARTE_FORHOLD),
            forventetHelsetilstandUtvikling?.toUtdypendeSporsmal(
                Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING
            ),
            medisinskeHensyn?.toUtdypendeSporsmal(Sporsmalstype.MEDISINSKE_HENSYN),
        )
        .ifEmpty { null }
}

private fun SykInnUtdypendeSporsmalSvar.toUtdypendeSporsmal(
    type: Sporsmalstype
): UtdypendeSporsmal =
    UtdypendeSporsmal(
        type = type,
        sporsmal = sporsmalstekst,
        svar = svar,
        skjermetForArbeidsgiver = true,
    )
