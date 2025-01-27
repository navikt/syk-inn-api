package no.nav.tsm.sykinnapi.modell.sykinn

import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult

data class SykInnApiResponse(val sykmeldingId: String, val validationResult: ValidationResult)
