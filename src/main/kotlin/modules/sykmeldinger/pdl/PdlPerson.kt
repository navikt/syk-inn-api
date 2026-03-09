package modules.sykmeldinger.pdl

import java.time.LocalDate

data class PdlPerson(val navn: PdlNavn?, val foedselsdato: LocalDate?, val identer: List<Ident>)

data class PdlNavn(val fornavn: String, val mellomnavn: String?, val etternavn: String)

data class Ident(val ident: String, val gruppe: IDENT_GRUPPE, val historisk: Boolean)

enum class IDENT_GRUPPE {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID,
}
