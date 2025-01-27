package no.nav.tsm.sykinnapi.service.sykmeldingHent

import java.time.LocalDate
import no.nav.tsm.sykinnapi.service.syfosmregister.SyfosmregisterService
import org.springframework.stereotype.Service

@Service
class SykmeldingHent(val syfosmregisterService: SyfosmregisterService) {

    fun get(sykmeldingId: String, hprNummer: String): SykmeldingKvittering {
        val sykmeldingDTO = syfosmregisterService.getSykmelding(sykmeldingId)

        if (sykmeldingDTO.behandler.hprNummer == hprNummer) {
            return SykmeldingKvittering(
                sykmeldingId = sykmeldingId,
                periode =
                    Periode(
                        fom = sykmeldingDTO.periode.fom,
                        tom = sykmeldingDTO.periode.tom,
                    ),
                pasient = Pasient(fnr = sykmeldingDTO.pasient.fnr),
                hovedDiagnose =
                    Diagnose(
                        code = sykmeldingDTO.hovedDiagnose.code,
                        system = sykmeldingDTO.hovedDiagnose.system,
                        text = sykmeldingDTO.hovedDiagnose.text,
                    ),
            )
        } else {
            throw RuntimeException("HPR-nummer matcher ikke behandler")
        }
    }
}

data class SykmeldingKvittering(
    val sykmeldingId: String,
    val periode: Periode,
    val pasient: Pasient,
    val hovedDiagnose: Diagnose
)

data class Periode(val fom: LocalDate, val tom: LocalDate)

data class Pasient(val fnr: String)

data class Diagnose(val code: String, val system: String, val text: String)
