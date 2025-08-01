package no.nav.tsm.syk_inn_api.sykmelding.response

import java.time.LocalDate

data class SykmeldingDocumentLight(
    val sykmeldingId: String,
    val meta: SykmeldingDocumentMeta,
    val values: SykmeldingDocumentLightValues,
    val utfall: SykmeldingDocumentRuleResult,
)

data class SykmeldingDocumentLightValues(
    val aktivitet: List<SykmeldingDocumentLightAktivitet>,
)

data class SykmeldingDocumentLightAktivitet(
    val fom: LocalDate,
    val tom: LocalDate,
    val type: String,
)
