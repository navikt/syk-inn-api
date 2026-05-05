package no.nav.tsm.modules.sykmeldinger.pdl

import java.time.LocalDate
import no.nav.tsm.core.common.Navn

data class PdlPerson(val navn: PdlNavn, val foedselsdato: LocalDate?, val identer: List<PdlIdent>)

data class PdlNavn(
    override val fornavn: String,
    override val mellomnavn: String?,
    override val etternavn: String,
) : Navn

data class PdlIdent(val ident: String, val gruppe: PdlIdentgruppe, val historisk: Boolean)

enum class PdlIdentgruppe {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID,
}
