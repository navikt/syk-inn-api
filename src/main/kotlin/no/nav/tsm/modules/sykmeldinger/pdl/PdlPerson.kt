package no.nav.tsm.modules.sykmeldinger.pdl

import java.time.LocalDate

data class PdlPerson(val navn: PdlNavn, val foedselsdato: LocalDate?, val identer: List<Ident>)

data class PdlNavn(val fornavn: String, val mellomnavn: String?, val etternavn: String)

data class Ident(val ident: String, val gruppe: Identgruppe, val historisk: Boolean)

enum class Identgruppe {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID,
}
