package modules.sykmeldinger.sykmelder

data class SykmelderMedHpr(
    val ident: String,
    val hprNummer: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    // TODO("Legg på liste med godkjenninger")

)
