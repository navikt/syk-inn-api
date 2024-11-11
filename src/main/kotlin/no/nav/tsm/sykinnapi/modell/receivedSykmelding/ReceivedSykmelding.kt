package no.nav.tsm.sykinnapi.modell.receivedSykmelding

import java.time.LocalDateTime

data class ReceivedSykmelding(
    val sykmelding: Sykmelding,
    val personNrPasient: String,
    val tlfPasient: String?,
    val personNrLege: String,
    val legeHelsepersonellkategori: String?,
    val legeHprNr: String?,
    val navLogId: String,
    val msgId: String,
    val legekontorOrgNr: String?,
    val legekontorHerId: String?,
    val legekontorReshId: String?,
    val legekontorOrgName: String,
    val mottattDato: LocalDateTime,
    val rulesetVersion: String?,
    val merknader: List<Merknad>?,
    val partnerreferanse: String?,
    val vedlegg: List<String>?,
    val utenlandskSykmelding: UtenlandskSykmelding?,
    /**
     * Full fellesformat as a XML payload, this is only used for infotrygd compat and should be
     * removed in thefuture
     */
    val fellesformat: String,
    /** TSS-ident, this is only used for infotrygd compat and should be removed in thefuture */
    val tssid: String?,
)

fun ReceivedSykmelding.toReceivedSykmeldingWithValidation(
    validationResult: ValidationResult
): ReceivedSykmeldingWithValidation {
    return ReceivedSykmeldingWithValidation(
        sykmelding = this.sykmelding,
        personNrPasient = this.personNrPasient,
        tlfPasient = this.tlfPasient,
        personNrLege = this.personNrLege,
        legeHelsepersonellkategori = this.legeHelsepersonellkategori,
        legeHprNr = this.legeHprNr,
        navLogId = this.navLogId,
        msgId = this.msgId,
        legekontorOrgNr = this.legekontorOrgNr,
        legekontorHerId = this.legekontorHerId,
        legekontorReshId = this.legekontorReshId,
        legekontorOrgName = this.legekontorOrgName,
        mottattDato = this.mottattDato,
        rulesetVersion = this.rulesetVersion,
        merknader = this.merknader,
        partnerreferanse = this.partnerreferanse,
        vedlegg = this.vedlegg,
        utenlandskSykmelding = this.utenlandskSykmelding,
        fellesformat = this.fellesformat,
        tssid = this.tssid,
        validationResult = validationResult,
    )
}
