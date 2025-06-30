package no.nav.tsm.syk_inn_api.utils

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class PoisonPills(
    @param:Value($$"${nais.cluster}") private val cluster: String,
) {
    val logger = logger()

    val devPoisonPills =
        setOf(
            "8a0e2777-c071-458c-ad24-c4a721659a3b",
        )

    fun isPoisoned(sykmeldingId: String): Boolean {
        return when (cluster) {
            "dev-gcp" -> devPoisonPills.contains(sykmeldingId)
            "prod-gcp" -> false
            "local" -> false
            else -> {
                logger.error(
                    "Unknown cluster: $cluster, cannot determine if sykmeldingId is poisoned"
                )
                false
            }
        }
    }
}
