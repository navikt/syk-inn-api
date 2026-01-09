package no.nav.tsm.syk_inn_api.sykmelding.rules.juridiskvurdering

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import no.nav.syfo.rules.juridiskvurdering.JuridiskHenvisning
import no.nav.syfo.rules.juridiskvurdering.JuridiskUtfall
import no.nav.syfo.rules.juridiskvurdering.JuridiskVurdering
import no.nav.syfo.rules.juridiskvurdering.Lovverk
import no.nav.tsm.regulus.regula.RegulaJuridiskHenvisning
import no.nav.tsm.regulus.regula.RegulaLovverk
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.regulus.regula.TreeResult
import no.nav.tsm.syk_inn_api.sykmelding.rules.JuridiskVurderingResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class JuridiskHenvisningService(
    @Value("\${env.sourceVersion}") private val versjonsKode: String,
) {
    companion object {
        val EVENT_NAME = "subsumsjon"
        val VERSION = "1.0.0"
        val KILDE = "syk-inn-api"
    }

    fun createJuridiskHenvisning(
        sykmeldingId: String,
        sykmeldtIdent: String,
        regulaResult: RegulaResult
    ): JuridiskVurderingResult {
        val juridiskeHenvisninger =
            regulaResult.results.mapNotNull { result ->
                result.juridisk?.let {
                    resultToJuridiskVurdering(
                        sykmeldingId = sykmeldingId,
                        sykmeldtIdent = sykmeldtIdent,
                        result = result,
                        juridisk = it
                    )
                }
            }
        return JuridiskVurderingResult(juridiskeHenvisninger)
    }

    private fun resultToJuridiskVurdering(
        sykmeldingId: String,
        sykmeldtIdent: String,
        result: TreeResult,
        juridisk: RegulaJuridiskHenvisning,
    ): JuridiskVurdering {
        return JuridiskVurdering(
            id = UUID.randomUUID().toString(),
            eventName = EVENT_NAME,
            version = VERSION,
            kilde = KILDE,
            versjonAvKode = versjonsKode,
            fodselsnummer = sykmeldtIdent,
            juridiskHenvisning =
                JuridiskHenvisning(
                    lovverk =
                        when (juridisk.lovverk) {
                            RegulaLovverk.FOLKETRYGDLOVEN -> Lovverk.FOLKETRYGDLOVEN
                            else ->
                                throw IllegalArgumentException("Ukjent lovverk ${juridisk.lovverk}")
                        },
                    paragraf = juridisk.paragraf,
                    ledd = juridisk.ledd,
                    punktum = juridisk.punktum,
                    bokstav = juridisk.bokstav,
                ),
            sporing = mapOf("sykmelding" to sykmeldingId),
            input = result.ruleInputs,
            utfall = toJuridiskUtfall(result.status),
            tidsstempel = ZonedDateTime.now(ZoneOffset.UTC),
        )
    }

    private fun toJuridiskUtfall(status: RegulaStatus) =
        when (status) {
            RegulaStatus.OK -> {
                JuridiskUtfall.VILKAR_OPPFYLT
            }
            RegulaStatus.INVALID -> {
                JuridiskUtfall.VILKAR_IKKE_OPPFYLT
            }
            RegulaStatus.MANUAL_PROCESSING -> {
                JuridiskUtfall.VILKAR_UAVKLART
            }
            else -> {
                JuridiskUtfall.VILKAR_UAVKLART
            }
        }
}
