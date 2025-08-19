package no.nav.tsm.syk_inn_api.person

import java.time.LocalDate

data class Person(
    val navn: Navn,
    val ident: String,
    val fodselsdato: LocalDate,
)

fun Person.displayName(): String =
    "${navn.fornavn}${if (navn.mellomnavn != null) " ${navn.mellomnavn} " else " "}${navn.etternavn}"

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)
