package no.nav.tsm.syk_inn_api.model.sykmelding

import no.nav.tsm.syk_inn_api.model.Sykmelder

data class SavedSykmelding(
    val sykmeldingId: String,
    val pasientFnr: String,
    val sykmelderHpr: String,
    val sykmelding: Sykmelding,
    val legekontorOrgnr: String,
)

data class NewAwesomeMaybeFinalThingWithAllFieldsSykmeldingToReturnToSOF(
    val sykmeldingId: String,
    val pasientFnr: String,
    val sykmelder: Sykmelder,
    val sykmelding: Sykmelding, // Inkluderer diagnose og aktiviet(fom tom)
    val legekontorOrgnr: String,
    val arbeidsgiverSattAvlege: String, // TODO må få det inn i payload først
)
