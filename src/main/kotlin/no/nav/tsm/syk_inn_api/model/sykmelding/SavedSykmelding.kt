package no.nav.tsm.syk_inn_api.model.sykmelding

import no.nav.tsm.syk_inn_api.model.Sykmelder

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

data class NewAwesomeMaybeFinalThingWithAllFieldsSykmeldingToReturnToSOF(
    val sykmeldingId: String,
    val pasientFnr: String,
    val sykmelder: Sykmelder,
    val sykmelding: Sykmelding, // Inkluderer diagnose og aktiviet(fom tom)
    val legekontorOrgnr: String,
    val arbeidsgiver:
        String, // TODO her trenger vi et objekt med info om arbeidsgiver, må vel slå opp i Aareg i
// så fall om vi skal ha det med.
)
