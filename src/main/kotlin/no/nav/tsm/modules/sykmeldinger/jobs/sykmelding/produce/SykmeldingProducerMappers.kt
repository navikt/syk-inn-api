package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce

import java.time.OffsetDateTime
import no.nav.tsm.core.common.SykInnDiagnoseSystem
import no.nav.tsm.modules.sykmeldinger.domain.SykInnAktivitet
import no.nav.tsm.modules.sykmeldinger.domain.SykInnDiagnoseInfo
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.SykInnUtdypendeSporsmal
import no.nav.tsm.modules.sykmeldinger.domain.SykInnUtdypendeSporsmalSvar
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.text
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.AktivitetIkkeMulig
import no.nav.tsm.sykmelding.input.core.model.AvsenderSystem
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem
import no.nav.tsm.sykmelding.input.core.model.DigitalMedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmeldingMetadata
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

fun VerifiedSykInnSykmelding.toInputRecord(): SykmeldingRecord {
    return SykmeldingRecord(
        metadata = Digital(orgnummer = meta.legekontorOrgnr),
        validation = result.toValidationResult(meta.mottatt),
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
                                fornavn = meta.pasient.fornavn,
                                mellomnavn = meta.pasient.mellomnavn,
                                etternavn = meta.pasient.etternavn,
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
                                fornavn = meta.behandler.fornavn,
                                mellomnavn = meta.behandler.mellomnavn,
                                etternavn = meta.behandler.etternavn,
                            ),
                        adresse = null,
                        ids = listOf(PersonId(type = PersonIdType.HPR, id = meta.behandler.hpr)),
                        kontaktinfo =
                            listOf(
                                Kontaktinfo(type = KontaktinfoType.TLF, value = meta.legekontorTlf)
                            ),
                    ),
                sykmelder =
                    Sykmelder(
                        // TODO dont hardcode
                        helsepersonellKategori = HelsepersonellKategori.LEGE,
                        ids = listOf(PersonId(type = PersonIdType.HPR, id = meta.behandler.hpr)),
                    ),
                // TODO dont hardcode
                arbeidsgiver = IngenArbeidsgiver(),
                // TODO
                tilbakedatering = null,
                // TODO
                bistandNav = null,
                // TODO
                utdypendeSporsmal = values.utdypendeSporsmal.toUtdypendeOpplysninger(),
            ),
    )
}

private fun SykInnSykmeldingRuleResult.toValidationResult(
    mottatt: OffsetDateTime
): ValidationResult =
    when (this) {
        is SykInnSykmeldingRuleResult.OK -> ValidationResult(RuleType.OK, mottatt, emptyList())
        is SykInnSykmeldingRuleResult.Outcome -> {
            val rule =
                when (type) {
                    RuleType.OK -> throw IllegalStateException("Rule with outcome can't be OK")
                    RuleType.PENDING ->
                        PendingRule(rule, mottatt, ValidationType.AUTOMATIC, Reason("TODO", "TODO"))

                    RuleType.INVALID ->
                        InvalidRule(rule, ValidationType.AUTOMATIC, mottatt, Reason("TODO", "TODO"))
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

private fun SykInnAktivitet.toAktivitet(): Aktivitet =
    when (this) {
        is SykInnAktivitet.IkkeMulig ->
            AktivitetIkkeMulig(
                fom = fom,
                tom = tom,
                // TODO
                medisinskArsak = null,
                arbeidsrelatertArsak = null,
            )

        is SykInnAktivitet.Gradert ->
            Gradert(fom = fom, tom = tom, grad = grad, reisetilskudd = reisetilskudd)
        is SykInnAktivitet.Avventende -> TODO()
        is SykInnAktivitet.Behandlingsdager -> TODO()
        is SykInnAktivitet.Reisetilskudd -> TODO()
    }

private fun SykInnUtdypendeSporsmal?.toUtdypendeOpplysninger(): List<UtdypendeSporsmal>? {
    if (this == null) return null

    // TODO: Sanity check mapping between these enums and distinct values
    return listOfNotNull(
            hensynPaArbeidsplassen?.toUtdypendeSporsmal(Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN),
            medisinskOppsummering?.toUtdypendeSporsmal(Sporsmalstype.MEDISINSK_OPPSUMMERING),
            utfordringerMedArbeid?.toUtdypendeSporsmal(Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID),
            sykdomsutvikling?.toUtdypendeSporsmal(Sporsmalstype.MEDISINSK_OPPSUMMERING),
            arbeidsrelaterteUtfordringer?.toUtdypendeSporsmal(
                Sporsmalstype.UTFORDRINGER_MED_ARBEID
            ),
            behandlingOgFremtidigArbeid?.toUtdypendeSporsmal(
                Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID
            ),
            uavklarteForhold?.toUtdypendeSporsmal(Sporsmalstype.UAVKLARTE_FORHOLD),
            oppdatertMedisinskStatus?.toUtdypendeSporsmal(Sporsmalstype.MEDISINSK_OPPSUMMERING),
            realistiskMestringArbeid?.toUtdypendeSporsmal(Sporsmalstype.UTFORDRINGER_MED_ARBEID),
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
