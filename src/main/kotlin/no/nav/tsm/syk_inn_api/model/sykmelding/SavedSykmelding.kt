package no.nav.tsm.syk_inn_api.model.sykmelding

data class SavedSykmelding(
    val sykmeldingId: String,
    val pasientFnr: String,
    val sykmelderHpr: String,
    val sykmelding: Sykmelding,
    val legekontorOrgnr: String,
)
// TODO denne klassen burde kanskje kunne brukes som ei liste mtp tidligere sykmeldinger for det
// endepunktet, då er det snakk om at en kanskje trenger følgande info:
// Periode fom tom
// diagnoser
// grad
// Arbeidsgiver
// Person
// sykmelder (kven som har skrevet sykmeldinga)
// TODO henting av 1 eller fleire sykmeldinger bør jo være samme objektet , berre at det er ei
// samling av. Kan være eit sett
