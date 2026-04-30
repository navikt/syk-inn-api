package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce

import java.time.OffsetDateTime
import no.nav.tsm.core.common.SykInnDiagnoseSystem
import no.nav.tsm.modules.sykmeldinger.domain.SykInnAktivitet
import no.nav.tsm.modules.sykmeldinger.domain.SykInnArbeidsgiver
import no.nav.tsm.modules.sykmeldinger.domain.SykInnDiagnoseInfo
import no.nav.tsm.modules.sykmeldinger.domain.SykInnMeldinger
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingMeta
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.SykInnTilbakedatering
import no.nav.tsm.modules.sykmeldinger.domain.SykInnUtdypendeSporsmal
import no.nav.tsm.modules.sykmeldinger.domain.SykInnUtdypendeSporsmalSvar
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.text
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.input.core.model.ArbeidsgiverInfo
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsak
import no.nav.tsm.sykmelding.input.core.model.AvsenderSystem
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.BistandNav
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem
import no.nav.tsm.sykmelding.input.core.model.DigitalMedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmeldingMetadata
import no.nav.tsm.sykmelding.input.core.model.EnArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.FlereArbeidsgivere
import no.nav.tsm.sykmelding.input.core.model.Gradert
import no.nav.tsm.sykmelding.input.core.model.IngenArbeidsgiver
import no.nav.tsm.sykmelding.input.core.model.InvalidRule
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.PendingRule
import no.nav.tsm.sykmelding.input.core.model.Reason
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.Sporsmalstype
import no.nav.tsm.sykmelding.input.core.model.Sykmelder
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.Tilbakedatering
import no.nav.tsm.sykmelding.input.core.model.UtdypendeSporsmal
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.ValidationType
import no.nav.tsm.sykmelding.input.core.model.Yrkesskade
import no.nav.tsm.sykmelding.input.core.model.metadata.Digital
import no.nav.tsm.sykmelding.input.core.model.metadata.HelsepersonellKategori
import no.nav.tsm.sykmelding.input.core.model.metadata.Kontaktinfo
import no.nav.tsm.sykmelding.input.core.model.metadata.KontaktinfoType
import no.nav.tsm.sykmelding.input.core.model.metadata.Navn
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonId
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType

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
                    "Should never map not digital sykmelding to SykmeldingRecord"
                )
        }
    return SykmeldingRecord(
        metadata = Digital(orgnummer = meta.legekontorOrgnr),
        validation = result.toValidationResult(meta.mottatt, reason),
        sykmelding =
            DigitalSykmelding(
                id = sykmeldingId.toString(),
                metadata =
                    DigitalSykmeldingMetadata(
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
                    DigitalMedisinskVurdering(
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
                                PersonId(type = PersonIdType.FNR, id = meta.behandler.fnr),
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
                        PendingRule(
                            rule,
                            mottatt,
                            ValidationType.AUTOMATIC,
                            reason
                                ?: throw IllegalStateException(
                                    "Rule with outcome 'PENDING' can't be without reason"
                                ),
                        )

                    RuleType.INVALID ->
                        InvalidRule(
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
            when (system) {
                SykInnDiagnoseSystem.ICPC2 -> DiagnoseSystem.ICPC2
                SykInnDiagnoseSystem.ICD10 -> DiagnoseSystem.ICD10
                SykInnDiagnoseSystem.ICPC2B -> DiagnoseSystem.ICPC2B
            },
        kode = code,
        tekst = this.text(),
    )

private fun SykInnTilbakedatering.toTilbakedatering(): Tilbakedatering =
    Tilbakedatering(kontaktDato = kontaktdato, begrunnelse = begrunnelse)

private fun SykInnMeldinger.toBistandNav(): BistandNav =
    BistandNav(
        beskrivBistand = tilNav,
        // TODO: Default false?
        bistandUmiddelbart = false,
    )

private fun List<String>.toHelsepersonellkategori(): HelsepersonellKategori =
    map { parseHelsepersonellKategori(it) }.minBy { helsepersonellkategoriPresedence(it) }

private fun SykInnAktivitet.toAktivitet(): Aktivitet =
    when (this) {
        is SykInnAktivitet.IkkeMulig ->
            AktivitetIkkeMulig(
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
            Gradert(fom = fom, tom = tom, grad = grad, reisetilskudd = reisetilskudd)

        // TODO
        is SykInnAktivitet.Avventende -> TODO()
        // TODO
        is SykInnAktivitet.Behandlingsdager -> TODO()
        // TODO
        is SykInnAktivitet.Reisetilskudd -> TODO()
    }

private fun SykInnArbeidsgiver?.toArbeidsgiver(meldingTilArbeidsgiver: String?): ArbeidsgiverInfo =
    when {
        this == null -> IngenArbeidsgiver()
        this.harFlere ->
            FlereArbeidsgivere(
                navn = this.arbeidsgivernavn,
                meldingTilArbeidsgiver = meldingTilArbeidsgiver,
                // syk-inn-api har ikke noe med disse verdiene å gjøre
                yrkesbetegnelse = null,
                stillingsprosent = null,
                tiltakArbeidsplassen = null,
            )

        else ->
            EnArbeidsgiver(
                meldingTilArbeidsgiver = meldingTilArbeidsgiver,
                // syk-inn-api har ikke noe med disse verdiene å gjøre
                navn = null,
                yrkesbetegnelse = null,
                stillingsprosent = null,
                tiltakArbeidsplassen = null,
            )
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
