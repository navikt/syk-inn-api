package no.nav.tsm.syk_inn_api.model

data class SavedSykmelding(
    val sykmeldingId: String,
    val pasientFnr: String,
    val sykmelderHpr: String,
    val sykmelding: Sykmelding,
    val legekontorOrgnr: String,
)
