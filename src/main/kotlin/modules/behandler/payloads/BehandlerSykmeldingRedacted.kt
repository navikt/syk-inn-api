package modules.behandler.payloads

import java.time.LocalDate

data class BehandlerSykmeldingRedacted(
    override val sykmeldingId: String,
    override val meta: BehandlerSykmeldingMeta,
    override val utfall: BehandlerSykmeldingRuleResult,
    val values: BehandlerSykmeldingRedactedValues,
) : BehandlerSykmelding

data class BehandlerSykmeldingRedactedValues(
    val aktivitet: List<SykmeldingDocumentRedactedAktivitet>
)

data class SykmeldingDocumentRedactedAktivitet(
    val fom: LocalDate,
    val tom: LocalDate,
    val type: String,
)
