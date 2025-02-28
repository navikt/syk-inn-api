package no.nav.tsm.sykinnapi.modell.smpdfgen

import java.time.LocalDateTime
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Merknad
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Sykmelding
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult

data class PdfPayload(
    val pasient: Pasient,
    val sykmelding: Sykmelding,
    val validationResult: ValidationResult,
    val mottattDato: LocalDateTime,
    val behandlerKontorOrgName: String,
    val merknader: List<Merknad>?,
    val rulesetVersion: String?,
    val signerendBehandlerHprNr: String?,
)

data class Pasient(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val personnummer: String,
    val tlfNummer: String?,
)
