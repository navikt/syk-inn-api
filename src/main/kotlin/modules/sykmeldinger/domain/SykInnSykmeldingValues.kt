package modules.sykmeldinger.domain

import java.time.LocalDate
import modules.behandler.payloads.SykInnDiagnoseSystem
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarsgrunn
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType

data class SykInnSykmeldingValues(
    val pasientenSkalSkjermes: Boolean,
    val hoveddiagnose: SykInnDiagnoseInfo,
    val bidiagnoser: List<SykInnDiagnoseInfo>,
    val aktivitet: List<SykInnAktivitet>,
    val svangerskapsrelatert: Boolean,
    val meldinger: SykInnMeldinger,
    val yrkesskade: SykInnYrkesskade?,
    val arbeidsgiver: SykInnArbeidsgiver?,
    val tilbakedatering: SykInnTilbakedatering?,
    val utdypendeSporsmal: SykInnUtdypendeSporsmal?,
    val annenFravarsgrunn: AnnenFravarsgrunn?,
)

data class SykInnMeldinger(val tilNav: String?, val tilArbeidsgiver: String?)

data class SykInnYrkesskade(val yrkesskade: Boolean, val skadedato: LocalDate?)

data class SykInnArbeidsgiver(val harFlere: Boolean, val arbeidsgivernavn: String)

data class SykInnTilbakedatering(val startdato: LocalDate, val begrunnelse: String)

data class SykInnUtdypendeSporsmal(
    val hensynPaArbeidsplassen: SykInnUtdypendeSporsmalSvar?,
    val medisinskOppsummering: SykInnUtdypendeSporsmalSvar?,
    val utfordringerMedArbeid: SykInnUtdypendeSporsmalSvar?,
    val sykdomsutvikling: SykInnUtdypendeSporsmalSvar?,
    val arbeidsrelaterteUtfordringer: SykInnUtdypendeSporsmalSvar?,
    val behandlingOgFremtidigArbeid: SykInnUtdypendeSporsmalSvar?,
    val uavklarteForhold: SykInnUtdypendeSporsmalSvar?,
    val oppdatertMedisinskStatus: SykInnUtdypendeSporsmalSvar?,
    val realistiskMestringArbeid: SykInnUtdypendeSporsmalSvar?,
    val forventetHelsetilstandUtvikling: SykInnUtdypendeSporsmalSvar?,
    val medisinskeHensyn: SykInnUtdypendeSporsmalSvar?,
)

data class SykInnUtdypendeSporsmalSvar(val sporsmalstekst: String, val svar: String)

data class SykInnMedisinskArsak(val isMedisinskArsak: Boolean)

data class SykInnArbeidsrelatertArsak(
    val isArbeidsrelatertArsak: Boolean,
    val arbeidsrelaterteArsaker: List<ArbeidsrelatertArsakType>,
    val annenArbeidsrelatertArsak: String?,
)

data class SykInnDiagnoseInfo(val system: SykInnDiagnoseSystem, val code: String)
