package no.nav.tsm.modules.sykmeldinger.rules.mappers

import no.nav.tsm.modules.sykmeldinger.domain.SykInnUtdypendeSporsmal

fun SykInnUtdypendeSporsmal.toRegulaBesvartUtdypende(): List<String> {
    if (this.medisinskeHensyn != null || this.forventetHelsetilstandUtvikling != null) {
        // Uke 39: 6.5 should have highest precedence
        return uke39() + uke17() + uke7()
    }

    if (this.uavklarteForhold != null || this.behandlingOgFremtidigArbeid != null) {
        // Uke 17: 6.4 should have highest precedence
        return uke17() + uke7()
    }

    // Uke 7 or no values:
    return uke7()
}

fun SykInnUtdypendeSporsmal.uke39() =
    listOfNotNull(
        this.medisinskOppsummering?.let { "6.5.1" },
        this.utfordringerMedArbeid?.let { "6.5.2" },
        this.forventetHelsetilstandUtvikling?.let { "6.5.3" },
        this.medisinskeHensyn?.let { "6.5.4" },
    )

fun SykInnUtdypendeSporsmal.uke17() =
    listOfNotNull(
        this.medisinskOppsummering?.let { "6.4.1" },
        this.utfordringerMedArbeid?.let { "6.4.2" },
        this.behandlingOgFremtidigArbeid?.let { "6.4.3" },
        this.uavklarteForhold?.let { "6.4.4" },
    )

fun SykInnUtdypendeSporsmal.uke7() =
    listOfNotNull(
        this.medisinskOppsummering?.let { "6.3.1" },
        this.utfordringerMedGradertArbeid?.let { "6.3.2" },
        this.hensynPaArbeidsplassen?.let { "6.3.3" },
    )
