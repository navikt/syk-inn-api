package no.nav.tsm.mottak.sykmelding.model.metadata

enum class OrgIdType {
    AKO,
    APO,
    AVD,
    ENH,
    HER,
    LAV,
    LIN,
    LOK,
    NPR,
    RSH,
    SYS,
    UGYLDIG;

    companion object {
        fun parse(type: String): OrgIdType {
            return when (type) {
                "AKO" -> AKO
                "APO" -> APO
                "AVD" -> AVD
                "ENH" -> ENH
                "HER" -> HER
                "LAV" -> LAV
                "LIN" -> LIN
                "LOK" -> LOK
                "NPR" -> NPR
                "RSH" -> RSH
                "SYS" -> SYS
                "her" -> UGYLDIG
                else -> throw IllegalArgumentException("Unknown OrgIdType: $type")
            }
        }
    }
}

enum class OrganisasjonsType {
    PRIVATE_SPESIALISTER_MED_DRIFTSAVTALER,
    TANNLEGE_TANNHELSE,
    IKKE_OPPGITT,
    UGYLDIG;

    companion object {
        fun parse(v: String?): OrganisasjonsType {
            return when (v) {
                "4" -> PRIVATE_SPESIALISTER_MED_DRIFTSAVTALER
                "110" -> TANNLEGE_TANNHELSE
                "NXU:IT" -> UGYLDIG
                "NXU:IT," -> UGYLDIG
                null -> IKKE_OPPGITT
                else -> throw IllegalArgumentException("Ukjent organisasjonstype: $v")
            }
        }
    }
}

data class OrgId(
    val id: String,
    val type: OrgIdType,
)

data class Organisasjon(
    val navn: String?,
    val type: OrganisasjonsType,
    val ids: List<OrgId>,
    val adresse: Adresse?,
    val kontaktinfo: List<Kontaktinfo>?,
    val underOrganisasjon: UnderOrganisasjon?,
    val helsepersonell: Helsepersonell?,
)

data class UnderOrganisasjon(
    val navn: String,
    val type: OrganisasjonsType,
    val adresse: Adresse?,
    val kontaktinfo: List<Kontaktinfo>,
    val ids: List<OrgId>,
)
