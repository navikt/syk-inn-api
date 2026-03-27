package no.nav.tsm.modules.behandler.payloads

import java.time.LocalDate
import java.util.UUID

data class BehandlerSykmeldingRedacted(
    override val sykmeldingId: UUID,
    override val meta: BehandlerSykmeldingMeta,
    val values: BehandlerSykmeldingRedactedValues,
    /** Should only ever be OK */
    val utfall: BehandlerSykmeldingRuleResult,
) : BehandlerSykmelding {
    val isFull: Boolean = false
}

data class BehandlerSykmeldingRedactedValues(
    val aktivitet: List<BehandlerSykmeldingRedactedAktivitet>
)

data class BehandlerSykmeldingRedactedAktivitet(
    val fom: LocalDate,
    val tom: LocalDate,
    val type: String,
)
