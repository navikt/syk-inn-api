package no.nav.tsm.sykinnapi.service.sykmeldingHent

import java.time.LocalDate
import no.nav.tsm.sykinnapi.service.syfosmregister.SyfosmregisterService
import org.springframework.stereotype.Service

@Service
class SykmeldingHent(val syfosmregisterService: SyfosmregisterService) {

    fun get(sykmeldingId: String, hprnummer: String): SykmeldingKvittering {
        val sykmeldingDTO = syfosmregisterService.getSykmelding(sykmeldingId)

        if (sykmeldingDTO.behandler.hpr == hprnummer) {
            return SykmeldingKvittering(
                sykmeldingId = sykmeldingId,
                periode =
                    Periode(
                        sykmeldingDTO.sykmeldingsperioder.first().fom,
                        sykmeldingDTO.sykmeldingsperioder.first().tom,
                    ),
                pasient =
                    Pasient("13421412414", "Per hansen"), // TODO lage nytt api i syfosmregister?
                hovedDiagnose =
                    Diagnose(
                        code = sykmeldingDTO.medisinskVurdering!!.hovedDiagnose!!.kode,
                        system = sykmeldingDTO.medisinskVurdering.hovedDiagnose!!.system,
                        text = sykmeldingDTO.medisinskVurdering.hovedDiagnose.tekst ?: "",
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

data class Pasient(val fnr: String, val navn: String)

data class Diagnose(val code: String, val system: String, val text: String)
