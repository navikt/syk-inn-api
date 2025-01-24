package no.nav.tsm.sykinnapi.modell.syfosmregister

enum class SvarRestriksjonDTO(
    val codeValue: String,
    val text: String,
    val oid: String = "2.16.578.1.12.4.1.1.8134"
) {
    SKJERMET_FOR_ARBEIDSGIVER("A", "Informasjonen skal ikke vises arbeidsgiver"),
    SKJERMET_FOR_PASIENT("P", "Informasjonen skal ikke vises pasient"),
    SKJERMET_FOR_NAV("N", "Informasjonen skal ikke vises NAV"),
}
