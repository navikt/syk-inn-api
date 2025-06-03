package no.nav.tsm.syk_inn_api.person.pdl

import java.time.LocalDate
import no.nav.tsm.syk_inn_api.common.Navn

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

enum class IDENT_GRUPPE {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID,
}
