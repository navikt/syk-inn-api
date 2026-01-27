package no.nav.tsm.syk_inn_api.person

import java.time.LocalDate

data class Person(
    val navn: Navn,
    val ident: String,
    val fodselsdato: LocalDate,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)
