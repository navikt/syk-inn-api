package no.nav.tsm.sykinnapi.service.sykmeldingByIdentHent

import no.nav.tsm.sykinnapi.service.syfosmregister.SyfosmregisterService
import no.nav.tsm.sykinnapi.service.sykmeldingKvitteringHent.Diagnose
import no.nav.tsm.sykinnapi.service.sykmeldingKvitteringHent.Pasient
import no.nav.tsm.sykinnapi.service.sykmeldingKvitteringHent.Periode
import no.nav.tsm.sykinnapi.service.sykmeldingKvitteringHent.SykmeldingKvittering
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SykmeldingByIdentService(val syfosmregisterService: SyfosmregisterService) {

    private val logger = LoggerFactory.getLogger(SykmeldingByIdentService::class.java)

    fun get(ident: String): List<SykmeldingKvittering> {
        logger.info(
            "Trying to fetch sykmelding for ident=$ident",
        )
        val sykmeldingDTO = syfosmregisterService.getSykmeldingByIdent(ident)

        return sykmeldingDTO.map { sykmelding ->
            SykmeldingKvittering(
                sykmeldingId = sykmelding.sykmeldingId,
                periode =
                    Periode(
                        fom = sykmelding.periode.fom,
                        tom = sykmelding.periode.tom,
                    ),
                pasient = Pasient(fnr = sykmelding.pasient.fnr),
                hovedDiagnose =
                    Diagnose(
                        code = sykmelding.hovedDiagnose.code,
                        system = sykmelding.hovedDiagnose.system,
                        text = sykmelding.hovedDiagnose.text,
                    ),
            )
        }
    }
}
