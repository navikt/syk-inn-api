package modules.sykmeldinger.rules

import core.logger
import core.otel.failSpan
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDateTime
import modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import modules.sykmeldinger.pdl.PdlClient
import modules.sykmeldinger.rules.mappers.mapPdlPersonToRegulaPasient
import modules.sykmeldinger.rules.mappers.mapSykmelderToRegulaBehandler
import modules.sykmeldinger.rules.mappers.mapUnruledSykInnSykmeldingToRegulaPayload
import modules.sykmeldinger.sykmelder.Sykmelder
import no.nav.tsm.regulus.regula.RegulaBehandler
import no.nav.tsm.regulus.regula.RegulaPasient
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.regulus.regula.executeRegulaRules
import no.nav.tsm.regulus.regula.executor.ExecutionMode
import no.nav.tsm.sykmelding.input.core.model.RuleType

class RuleService(private val pdlClient: PdlClient) {
    private val logger = logger()

    @WithSpan
    suspend fun verify(
        sykmelding: UnverifiedSykInnSykmelding,
        sykmelder: Sykmelder,
    ): SykInnSykmeldingRuleResult {
        val now = LocalDateTime.now()
        val pasient =
            pdlClient.getPerson(sykmelding.meta.pasientIdent)
                ?: throw IllegalStateException("Unable to execute rules for pasient not in PDL")
        val regulaPasient =
            pasient.mapPdlPersonToRegulaPasient()
                ?: throw IllegalStateException(
                    "Unable to execute rules for pasient with missing or invalid ident or fødselsdato in PDL"
                )
        val regulaBehandler =
            sykmelder.mapSykmelderToRegulaBehandler(sykmelding.meta.legekontorOrgnr)

        return this.executeRegulaRules(
            behandletTidspunkt = now,
            sykmelding = sykmelding,
            behandler = regulaBehandler,
            pasient = regulaPasient,
        )
    }

    @WithSpan
    private fun executeRegulaRules(
        behandletTidspunkt: LocalDateTime,
        sykmelding: UnverifiedSykInnSykmelding,
        behandler: RegulaBehandler,
        pasient: RegulaPasient,
    ): SykInnSykmeldingRuleResult {
        return try {
            val regulaExecutionPayload =
                mapUnruledSykInnSykmeldingToRegulaPayload(
                    behandletTidspunkt = behandletTidspunkt,
                    sykmelding = sykmelding,
                    behandler = behandler,
                    pasient = pasient,
                )

            val result =
                executeRegulaRules(
                    ruleExecutionPayload = regulaExecutionPayload,
                    mode = ExecutionMode.NORMAL,
                )

            logger.info(
                "Sykmelding med id=${sykmelding.sykmeldingId} er validering ${result.status.name} mot regler"
            )

            result.toSykInnRuleResult()
        } catch (e: Exception) {
            throw RuntimeException(
                    "Error while executing Regula rules for sykmeldingId=${sykmelding.sykmeldingId}",
                    e,
                )
                .failSpan()
        }
    }

    private fun RegulaResult.toSykInnRuleResult(): SykInnSykmeldingRuleResult =
        when (this) {
            is RegulaResult.Ok -> SykInnSykmeldingRuleResult.OK()
            is RegulaResult.NotOk ->
                SykInnSykmeldingRuleResult.Outcome(
                    message = this.outcome.reason.sykmelder,
                    type =
                        when (this.status) {
                            RegulaStatus.MANUAL_PROCESSING -> RuleType.PENDING
                            RegulaStatus.INVALID -> RuleType.INVALID
                            RegulaStatus.OK ->
                                throw IllegalStateException(
                                    "RegulaResult.NotOk cannot have status OK"
                                )
                        },
                )
        }
}
