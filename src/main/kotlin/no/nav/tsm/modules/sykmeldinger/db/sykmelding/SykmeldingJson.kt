package no.nav.tsm.modules.sykmeldinger.db.sykmelding

import no.nav.tsm.core.common.name.Navn
import no.nav.tsm.modules.behandler.payloads.SykInnDiagnoseSystem
import no.nav.tsm.modules.sykmeldinger.domain.SykInnAktivitet
import no.nav.tsm.modules.sykmeldinger.domain.SykInnArbeidsgiver
import no.nav.tsm.modules.sykmeldinger.domain.SykInnArbeidsrelatertArsak
import no.nav.tsm.modules.sykmeldinger.domain.SykInnDiagnoseInfo
import no.nav.tsm.modules.sykmeldinger.domain.SykInnMedisinskArsak
import no.nav.tsm.modules.sykmeldinger.domain.SykInnMeldinger
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.SykInnTilbakedatering
import no.nav.tsm.modules.sykmeldinger.domain.SykInnUtdypendeSporsmal
import no.nav.tsm.modules.sykmeldinger.domain.SykInnUtdypendeSporsmalSvar
import no.nav.tsm.modules.sykmeldinger.domain.SykInnYrkesskade
import no.nav.tsm.modules.sykmeldinger.domain.text
import no.nav.tsm.sykmelding.input.core.model.RuleType

/**
 * Any mappers that go from the domain types to database internal jsonb objects
 *
 * domain → jsonb
 */
object toJsonb {
    fun SykInnSykmeldingRuleResult.toRuleResultJson(): SykmeldingJsonbRuleResult =
        when (this) {
            is SykInnSykmeldingRuleResult.OK -> SykmeldingJsonbRuleResult(RuleType.OK, null, null)
            is SykInnSykmeldingRuleResult.Outcome ->
                SykmeldingJsonbRuleResult(
                    type = this.type,
                    message = this.message,
                    rule = this.rule,
                )
        }

    fun SykInnDiagnoseInfo?.toDiagnoseJsonb(): SykmeldingJsonbDiagnose? =
        this?.let {
            SykmeldingJsonbDiagnose(system = it.system.name, text = it.text(), code = it.code)
        }

    fun Navn.toNavnJsonb(): SykmeldingJsonbNavn =
        SykmeldingJsonbNavn(fornavn = fornavn, mellomnavn = mellomnavn, etternavn = etternavn)

    fun SykInnMeldinger?.toMeldingerJsonb(): SykmeldingJsonbMeldinger? =
        this?.let { SykmeldingJsonbMeldinger(tilNav = tilNav, tilArbeidsgiver = tilArbeidsgiver) }

    fun SykInnYrkesskade?.toYrkesskadeJsonb(): SykmeldingJsonbYrkesskade? =
        this?.let {
            SykmeldingJsonbYrkesskade(yrkesskade = it.yrkesskade, skadedato = it.skadedato)
        }

    fun SykInnArbeidsgiver?.toArbeidsgiverJsonb(): SykmeldingJsonbArbeidsgiver? =
        this?.let {
            SykmeldingJsonbArbeidsgiver(
                harFlere = it.harFlere,
                arbeidsgivernavn = it.arbeidsgivernavn,
            )
        }

    fun SykInnTilbakedatering?.toTilbakedateringJsonb(): SykmeldingJsonbTilbakedatering? =
        this?.let {
            SykmeldingJsonbTilbakedatering(startdato = it.startdato, begrunnelse = it.begrunnelse)
        }

    fun SykInnUtdypendeSporsmal?.toUtdypendeSporsmalJsonb():
        Map<String, SykmeldingJsonbUtdypendeSporsmal>? {
        if (this == null) return null
        return buildMap {
                hensynPaArbeidsplassen?.let {
                    put(
                        "hensynPaArbeidsplassen",
                        SykmeldingJsonbUtdypendeSporsmal(it.sporsmalstekst, it.svar),
                    )
                }
                medisinskOppsummering?.let {
                    put(
                        "medisinskOppsummering",
                        SykmeldingJsonbUtdypendeSporsmal(it.sporsmalstekst, it.svar),
                    )
                }
                utfordringerMedArbeid?.let {
                    put(
                        "utfordringerMedArbeid",
                        SykmeldingJsonbUtdypendeSporsmal(it.sporsmalstekst, it.svar),
                    )
                }
                sykdomsutvikling?.let {
                    put(
                        "sykdomsutvikling",
                        SykmeldingJsonbUtdypendeSporsmal(it.sporsmalstekst, it.svar),
                    )
                }
                arbeidsrelaterteUtfordringer?.let {
                    put(
                        "arbeidsrelaterteUtfordringer",
                        SykmeldingJsonbUtdypendeSporsmal(it.sporsmalstekst, it.svar),
                    )
                }
                behandlingOgFremtidigArbeid?.let {
                    put(
                        "behandlingOgFremtidigArbeid",
                        SykmeldingJsonbUtdypendeSporsmal(it.sporsmalstekst, it.svar),
                    )
                }
                uavklarteForhold?.let {
                    put(
                        "uavklarteForhold",
                        SykmeldingJsonbUtdypendeSporsmal(it.sporsmalstekst, it.svar),
                    )
                }
                oppdatertMedisinskStatus?.let {
                    put(
                        "oppdatertMedisinskStatus",
                        SykmeldingJsonbUtdypendeSporsmal(it.sporsmalstekst, it.svar),
                    )
                }
                realistiskMestringArbeid?.let {
                    put(
                        "realistiskMestringArbeid",
                        SykmeldingJsonbUtdypendeSporsmal(it.sporsmalstekst, it.svar),
                    )
                }
                forventetHelsetilstandUtvikling?.let {
                    put(
                        "forventetHelsetilstandUtvikling",
                        SykmeldingJsonbUtdypendeSporsmal(it.sporsmalstekst, it.svar),
                    )
                }
                medisinskeHensyn?.let {
                    put(
                        "medisinskeHensyn",
                        SykmeldingJsonbUtdypendeSporsmal(it.sporsmalstekst, it.svar),
                    )
                }
            }
            .takeIf { it.isNotEmpty() }
    }
}

/**
 * Any mappers that go from database internal jsonb objects to the domain types
 *
 * jsonb → domain
 */
object fromJsonb {

    fun SykmeldingJsonbRuleResult.toSykInnResult(): SykInnSykmeldingRuleResult =
        when (this.type) {
            RuleType.OK -> SykInnSykmeldingRuleResult.OK()
            RuleType.PENDING,
            RuleType.INVALID ->
                SykInnSykmeldingRuleResult.Outcome(
                    this.type,
                    requireNotNull(this.message) { "${this.type} should always have message" },
                    requireNotNull(this.rule) { "${this.type} should have rule" },
                )
        }

    fun SykmeldingJsonbDiagnose.toSykInnDiagnose(): SykInnDiagnoseInfo =
        SykInnDiagnoseInfo(system = SykInnDiagnoseSystem.valueOf(this.system), code = this.code)

    fun SykmeldingJsonbMeldinger.toSykInnMeldinger(): SykInnMeldinger =
        SykInnMeldinger(tilNav = tilNav, tilArbeidsgiver = tilArbeidsgiver)

    fun SykmeldingJsonbYrkesskade.toSykInnYrkesskade(): SykInnYrkesskade =
        SykInnYrkesskade(yrkesskade = yrkesskade, skadedato = skadedato)

    fun SykmeldingJsonbArbeidsgiver.toSykInnArbeidsgiver(): SykInnArbeidsgiver =
        SykInnArbeidsgiver(harFlere = harFlere, arbeidsgivernavn = arbeidsgivernavn)

    fun SykmeldingJsonbTilbakedatering.toSykInnTilbakedatering(): SykInnTilbakedatering =
        SykInnTilbakedatering(startdato = startdato, begrunnelse = begrunnelse)

    fun Map<String, SykmeldingJsonbUtdypendeSporsmal>.toSykInnUtdypendeSporsmal():
        SykInnUtdypendeSporsmal =
        SykInnUtdypendeSporsmal(
            hensynPaArbeidsplassen = this["hensynPaArbeidsplassen"]?.toSvar(),
            medisinskOppsummering = this["medisinskOppsummering"]?.toSvar(),
            utfordringerMedArbeid = this["utfordringerMedArbeid"]?.toSvar(),
            sykdomsutvikling = this["sykdomsutvikling"]?.toSvar(),
            arbeidsrelaterteUtfordringer = this["arbeidsrelaterteUtfordringer"]?.toSvar(),
            behandlingOgFremtidigArbeid = this["behandlingOgFremtidigArbeid"]?.toSvar(),
            uavklarteForhold = this["uavklarteForhold"]?.toSvar(),
            oppdatertMedisinskStatus = this["oppdatertMedisinskStatus"]?.toSvar(),
            realistiskMestringArbeid = this["realistiskMestringArbeid"]?.toSvar(),
            forventetHelsetilstandUtvikling = this["forventetHelsetilstandUtvikling"]?.toSvar(),
            medisinskeHensyn = this["medisinskeHensyn"]?.toSvar(),
        )

    private fun SykmeldingJsonbUtdypendeSporsmal.toSvar(): SykInnUtdypendeSporsmalSvar =
        SykInnUtdypendeSporsmalSvar(sporsmalstekst = sporsmalstekst, svar = svar)

    fun SykInnAktivitet.toAktivitetJsonb(): SykmeldingJsonbAktivitet =
        when (this) {
            is SykInnAktivitet.IkkeMulig ->
                SykmeldingJsonbAktivitet.IkkeMulig(
                    fom = fom,
                    tom = tom,
                    medisinskArsak = SykmeldingJsonbMedisinskArsak(medisinskArsak.isMedisinskArsak),
                    arbeidsrelatertArsak =
                        SykmeldingJsonbArbeidsrelatertArsak(
                            isArbeidsrelatertArsak = arbeidsrelatertArsak.isArbeidsrelatertArsak,
                            arbeidsrelaterteArsaker = arbeidsrelatertArsak.arbeidsrelaterteArsaker,
                            annenArbeidsrelatertArsak =
                                arbeidsrelatertArsak.annenArbeidsrelatertArsak,
                        ),
                )

            is SykInnAktivitet.Gradert ->
                SykmeldingJsonbAktivitet.Gradert(
                    grad = grad,
                    fom = fom,
                    tom = tom,
                    reisetilskudd = reisetilskudd,
                )

            is SykInnAktivitet.Behandlingsdager ->
                SykmeldingJsonbAktivitet.Behandlingsdager(
                    antallBehandlingsdager = antallBehandlingsdager,
                    fom = fom,
                    tom = tom,
                )

            is SykInnAktivitet.Avventende ->
                SykmeldingJsonbAktivitet.Avventende(
                    innspillTilArbeidsgiver = innspillTilArbeidsgiver,
                    fom = fom,
                    tom = tom,
                )

            is SykInnAktivitet.Reisetilskudd ->
                SykmeldingJsonbAktivitet.Reisetilskudd(fom = fom, tom = tom)
        }

    fun SykmeldingJsonbAktivitet.toSykInnAktivitet(): SykInnAktivitet =
        when (this) {
            is SykmeldingJsonbAktivitet.IkkeMulig ->
                SykInnAktivitet.IkkeMulig(
                    fom = fom,
                    tom = tom,
                    medisinskArsak = SykInnMedisinskArsak(medisinskArsak.isMedisinskArsak),
                    arbeidsrelatertArsak =
                        SykInnArbeidsrelatertArsak(
                            isArbeidsrelatertArsak = arbeidsrelatertArsak.isArbeidsrelatertArsak,
                            arbeidsrelaterteArsaker = arbeidsrelatertArsak.arbeidsrelaterteArsaker,
                            annenArbeidsrelatertArsak =
                                arbeidsrelatertArsak.annenArbeidsrelatertArsak,
                        ),
                )

            is SykmeldingJsonbAktivitet.Gradert ->
                SykInnAktivitet.Gradert(
                    grad = grad,
                    fom = fom,
                    tom = tom,
                    reisetilskudd = reisetilskudd,
                )

            is SykmeldingJsonbAktivitet.Behandlingsdager ->
                SykInnAktivitet.Behandlingsdager(
                    antallBehandlingsdager = antallBehandlingsdager,
                    fom = fom,
                    tom = tom,
                )

            is SykmeldingJsonbAktivitet.Avventende ->
                SykInnAktivitet.Avventende(
                    innspillTilArbeidsgiver = innspillTilArbeidsgiver,
                    fom = fom,
                    tom = tom,
                )

            is SykmeldingJsonbAktivitet.Reisetilskudd ->
                SykInnAktivitet.Reisetilskudd(fom = fom, tom = tom)
        }
}
