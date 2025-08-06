package no.nav.tsm.syk_inn_api.sykmelding.response

import java.time.LocalDate

data class SykmeldingDocumentRedacted(
    val sykmeldingId: String,
    val meta: SykmeldingDocumentMeta,
    val values: SykmeldingDocumentRedactedValues,
    val utfall: SykmeldingDocumentRuleResult,
) : SykmeldingResponse

data class SykmeldingDocumentRedactedValues(
    val aktivitet: List<SykmeldingDocumentRedactedAktivitet>,
)

data class SykmeldingDocumentRedactedAktivitet(
    val fom: LocalDate,
    val tom: LocalDate,
    val type: String,
)
