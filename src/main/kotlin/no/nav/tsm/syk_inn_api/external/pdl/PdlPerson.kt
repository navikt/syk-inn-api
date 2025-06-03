package no.nav.tsm.syk_inn_api.external.pdl

import java.time.LocalDate

data class PdlPerson(
    val navn: Navn?,
    val foedselsdato: LocalDate?,
    val identer: List<Ident>,
)

data class Ident(
    val ident: String,
    val gruppe: IDENT_GRUPPE,
    val historisk: Boolean,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

enum class IDENT_GRUPPE {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID,
}
