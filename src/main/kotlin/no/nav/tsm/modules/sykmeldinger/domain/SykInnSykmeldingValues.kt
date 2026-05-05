package no.nav.tsm.modules.sykmeldinger.domain

import java.time.LocalDate
import no.nav.tsm.core.common.SykInnDiagnoseSystem
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarsgrunn
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType

data class SykInnSykmeldingValues(
    val pasientenSkalSkjermes: Boolean,
    val hoveddiagnose: SykInnDiagnoseInfo?,
    val bidiagnoser: List<SykInnDiagnoseInfo>,
    val aktivitet: List<SykInnAktivitet>,
    val svangerskapsrelatert: Boolean,
    val meldinger: SykInnMeldinger?,
    val yrkesskade: SykInnYrkesskade?,
    val arbeidsgiver: SykInnArbeidsgiver?,
    val tilbakedatering: SykInnTilbakedatering?,
    val utdypendeSporsmal: SykInnUtdypendeSporsmal?,
    val annenFravarsgrunn: AnnenFravarsgrunn?,
)

data class SykInnMeldinger(val tilNav: String?, val tilArbeidsgiver: String?)

data class SykInnYrkesskade(val yrkesskade: Boolean, val skadedato: LocalDate?)

data class SykInnArbeidsgiver(val harFlere: Boolean, val arbeidsgivernavn: String?)

data class SykInnTilbakedatering(val kontaktdato: LocalDate?, val begrunnelse: String?)

data class SykInnUtdypendeSporsmal(
    val hensynPaArbeidsplassen: SykInnUtdypendeSporsmalSvar?,
    val medisinskOppsummering: SykInnUtdypendeSporsmalSvar?,
    val utfordringerMedGradertArbeid: SykInnUtdypendeSporsmalSvar?,
    val utfordringerMedArbeid: SykInnUtdypendeSporsmalSvar?,
    val behandlingOgFremtidigArbeid: SykInnUtdypendeSporsmalSvar?,
    val uavklarteForhold: SykInnUtdypendeSporsmalSvar?,
    val forventetHelsetilstandUtvikling: SykInnUtdypendeSporsmalSvar?,
    val medisinskeHensyn: SykInnUtdypendeSporsmalSvar?,
)

data class SykInnUtdypendeSporsmalSvar(val sporsmalstekst: String, val svar: String)

data class SykInnArbeidsrelatertArsak(
    val arbeidsrelaterteArsaker: List<ArbeidsrelatertArsakType>,
    val annenArbeidsrelatertArsak: String?,
)

sealed interface SykInnDiagnoseInfo {
    val system: SykInnDiagnoseSystem
    val code: String
    val maybeTekst: String?
        get() =
            when (this) {
                is Invalid -> this.tekst
                is Valid -> this.tekst
            }

    /**
     * Represents a diagnose that is valid _today_ with the current systems and will be guaranteed
     * to have a text.
     */
    data class Valid(
        override val system: SykInnDiagnoseSystem,
        override val code: String,
        val tekst: String,
    ) : SykInnDiagnoseInfo

    /**
     * Represents a diagnose that is currently not in any kodeverk, or that has never been valid
     * (INVALID sykmelding)
     */
    data class Invalid(
        override val system: SykInnDiagnoseSystem,
        override val code: String,
        val tekst: String?,
    ) : SykInnDiagnoseInfo

    companion object {
        fun requireValid(system: SykInnDiagnoseSystem, code: String) =
            Valid(
                system = system,
                code = code,
                tekst =
                    system.maybeText(code)
                        ?: throw IllegalStateException(
                            "Kunne ikke opprette SykInnDiagnoseInfo.Valid: finner ikke diagnose for system $this og code $code"
                        ),
            )

        fun tryParse(
            system: SykInnDiagnoseSystem,
            code: String,
            fallbackText: String?,
        ): SykInnDiagnoseInfo {
            val text: String? = system.maybeText(code)

            return when (text) {
                null -> Invalid(system, code, fallbackText)
                else -> Valid(system, code, text)
            }
        }
    }
}
