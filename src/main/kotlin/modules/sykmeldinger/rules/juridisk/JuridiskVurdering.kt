package no.nav.syfo.rules.juridiskvurdering

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import no.nav.tsm.regulus.regula.*

data class JuridiskVurderingResult(val juridiskeVurderinger: List<JuridiskVurdering>)

enum class JuridiskVurderingStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED,
}

data class JuridiskVurdering(
    val juridiskHenvisning: JuridiskHenvisning,
    val input: Map<String, Any>,
    val tidsstempel: ZonedDateTime,
    val utfall: JuridiskUtfall,
)

data class JuridiskHenvisning(
    val lovverk: Lovverk,
    val paragraf: String,
    val ledd: Int?,
    val punktum: Int?,
    val bokstav: String?,
)

enum class Lovverk(val navn: String, val kortnavn: String, val lovverksversjon: LocalDate) {
    FOLKETRYGDLOVEN(
        navn = "Lov om folketrygd",
        kortnavn = "Folketrygdloven",
        lovverksversjon = LocalDate.of(2022, 1, 1),
    )
}

enum class JuridiskUtfall {
    VILKAR_OPPFYLT,
    VILKAR_IKKE_OPPFYLT,
    VILKAR_UAVKLART,
}

fun RegulaResult.toJuridiskVurdering(): JuridiskVurderingResult {
    val juridiskeHenvisninger = results.mapNotNull { result ->
        result.juridisk?.let { resultToJuridiskVurdering(result = result, juridisk = it) }
    }
    return JuridiskVurderingResult(juridiskeHenvisninger)
}

private fun resultToJuridiskVurdering(
    result: TreeResult,
    juridisk: RegulaJuridiskHenvisning,
): JuridiskVurdering {
    return JuridiskVurdering(
        juridiskHenvisning =
            JuridiskHenvisning(
                lovverk =
                    when (juridisk.lovverk) {
                        RegulaLovverk.FOLKETRYGDLOVEN -> Lovverk.FOLKETRYGDLOVEN
                        else -> throw IllegalArgumentException("Ukjent lovverk ${juridisk.lovverk}")
                    },
                paragraf = juridisk.paragraf,
                ledd = juridisk.ledd,
                punktum = juridisk.punktum,
                bokstav = juridisk.bokstav,
            ),
        input = result.ruleInputs,
        utfall = result.status.toJuridiskUtfall(),
        tidsstempel = ZonedDateTime.now(ZoneOffset.UTC),
    )
}

private fun RegulaStatus.toJuridiskUtfall() =
    when (this) {
        RegulaStatus.OK -> {
            JuridiskUtfall.VILKAR_OPPFYLT
        }
        RegulaStatus.INVALID -> {
            JuridiskUtfall.VILKAR_IKKE_OPPFYLT
        }
        RegulaStatus.MANUAL_PROCESSING -> {
            JuridiskUtfall.VILKAR_UAVKLART
        }
    }
