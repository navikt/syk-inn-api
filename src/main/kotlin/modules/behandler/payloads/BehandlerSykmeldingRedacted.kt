package modules.behandler.payloads

import java.time.LocalDate
import java.util.UUID

data class BehandlerSykmeldingRedacted(
    override val sykmeldingId: UUID,
    override val meta: BehandlerSykmeldingMeta,
    val values: BehandlerSykmeldingRedactedValues,
) : BehandlerSykmelding

data class BehandlerSykmeldingRedactedValues(
    val aktivitet: List<BehandlerSykmeldingRedactedAktivitet>
)

data class BehandlerSykmeldingRedactedAktivitet(
    val fom: LocalDate,
    val tom: LocalDate,
    val type: String,
)
