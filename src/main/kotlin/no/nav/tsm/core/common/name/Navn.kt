package no.nav.tsm.core.common.name

interface Navn {
    val fornavn: String
    val mellomnavn: String?
    val etternavn: String

    fun displayName(): String =
        "${fornavn}${if (mellomnavn != null) " ${mellomnavn} " else " "}${etternavn}"
}

data class SimpleNavn(
    override val fornavn: String,
    override val mellomnavn: String?,
    override val etternavn: String,
) : Navn
