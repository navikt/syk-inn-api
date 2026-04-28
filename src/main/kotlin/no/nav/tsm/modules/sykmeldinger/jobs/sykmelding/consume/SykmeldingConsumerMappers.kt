package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import java.time.LocalDate
import java.util.*
import no.nav.tsm.core.common.SykInnDiagnoseSystem
import no.nav.tsm.core.logger
import no.nav.tsm.modules.sykmeldinger.domain.*
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SupportedSpmType.*
import no.nav.tsm.sykmelding.input.core.model.*
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.metadata.*

private val log = logger()

fun SykmeldingRecord.toVerifiedSykmelding(): VerifiedSykInnSykmelding {

    val type =
        when (this.sykmelding.type) {
            SykmeldingType.DIGITAL -> SykInnSykmeldingType.DIGITAL
            SykmeldingType.XML -> SykInnSykmeldingType.XML
            SykmeldingType.PAPIR -> SykInnSykmeldingType.PAPIR
            SykmeldingType.UTENLANDSK -> SykInnSykmeldingType.UTENLANDSK
        }

    return VerifiedSykInnSykmelding(
        sykmeldingId = UUID.fromString(sykmelding.id),
        values = toSykmeldingValues(),
        meta = toMetadata(),
        result = validation.toResult(),
        type = type,
    )
}

private fun ValidationResult.toResult(): SykInnSykmeldingRuleResult =
    when (this.status) {
        RuleType.OK -> SykInnSykmeldingRuleResult.OK
        RuleType.PENDING,
        RuleType.INVALID -> {
            when (val rule = this.rules.firstOrNull { it !is OKRule }) {
                null ->
                    throw IllegalStateException(
                        "ValidationResult status=$status but no non-OK rule present"
                    )
                is InvalidRule ->
                    SykInnSykmeldingRuleResult.Outcome(
                        type = status,
                        rule = rule.name,
                        message = rule.reason.sykmelder,
                    )
                is PendingRule ->
                    SykInnSykmeldingRuleResult.Outcome(
                        type = status,
                        rule = rule.name,
                        message = rule.reason.sykmelder,
                    )
                is OKRule -> error("unreachable: filtered above")
            }
        }
    }

private fun Sykmelder.toSykInnBehandler(behandler: Behandler): SykInnBehandler {
    val behandlerHpr = behandler.ids.firstOrNull { it.type == PersonIdType.HPR }?.id
    val behandlerFnr = behandler.ids.firstOrNull { it.type == PersonIdType.FNR }?.id
    val sykmelderHpr = ids.firstOrNull { it.type == PersonIdType.HPR }?.id
    val sykmelderFnr = ids.firstOrNull { it.type == PersonIdType.FNR }?.id

    val isSame =
        (sykmelderHpr != null && sykmelderHpr == behandlerHpr) ||
            (sykmelderFnr != null && sykmelderFnr == behandlerFnr)
    val name = if (isSame) behandler.navn else null
    val hpr: String =
        (if (isSame) sykmelderHpr ?: behandlerHpr else sykmelderHpr)
            ?: throw IllegalArgumentException("sykmelder hpr can't be null")
    val fnr: String =
        (if (isSame) sykmelderFnr ?: behandlerFnr else sykmelderFnr)
            ?: throw IllegalArgumentException("sykmelder fnr can't be null")
    return SykInnBehandler(
        fornavn = name?.fornavn,
        mellomnavn = name?.mellomnavn,
        etternavn = name?.etternavn,
        hpr = hpr,
        fnr = fnr,
        helsepersonellkategori = listOf(this.helsepersonellKategori.toShortCode()),
    )
}

private fun SykmeldingRecord.toMetadata(): SykInnSykmeldingMeta {

    val source = sykmelding.metadata.avsenderSystem.navn
    val mottattDato = sykmelding.metadata.mottattDato
    val pasient = sykmelding.pasient.toSykInnPasient()
    val legekontorTlf = sykmelding.toLegekontorTlf()

    val (meta: MessageMetadata, sykmelding: Sykmelding) = this.metadata to this.sykmelding

    return when {
        meta is Digital && sykmelding is DigitalSykmelding -> {
            SykInnSykmeldingMeta.Digital(
                source = source,
                mottatt = mottattDato,
                pasient = pasient,
                behandler =
                    sykmelding.behandler.toSykInnBehandler(
                        sykmelding.sykmelder.helsepersonellKategori.toShortCode()
                    ),
                legekontorOrgnr = meta.orgnummer,
                legekontorTlf =
                    sykmelding.toLegekontorTlf()
                        ?: throw IllegalStateException(
                            "legekontorTlf must be set in DigitalSykmelding"
                        ),
            )
        }
        meta is Utenlandsk && sykmelding is UtenlandskSykmelding -> {
            SykInnSykmeldingMeta.Utenlandsk(
                source = source,
                mottatt = mottattDato,
                pasient = pasient,
            )
        }
        meta is Papir && sykmelding is Papirsykmelding -> {
            SykInnSykmeldingMeta.Legacy(
                source = source,
                mottatt = mottattDato,
                pasient = pasient,
                behandler = sykmelding.sykmelder.toSykInnBehandler(sykmelding.behandler),
                legekontorTlf = sykmelding.toLegekontorTlf(),
                legekontorOrgnr = getOrgNr(meta.sender),
            )
        }

        meta is EDIEmottak && sykmelding is XmlSykmelding -> {
            SykInnSykmeldingMeta.Legacy(
                source = source,
                mottatt = mottattDato,
                pasient = pasient,
                behandler = sykmelding.sykmelder.toSykInnBehandler(sykmelding.behandler),
                legekontorTlf = sykmelding.toLegekontorTlf(),
                legekontorOrgnr = getOrgNr(meta.sender),
            )
        }
        else -> {
            throw IllegalStateException(
                "Sykmelding: ${sykmelding.id} of type ${sykmelding.type} with metadata ${metadata.type} is not supported"
            )
        }
    }
}

private fun Pasient.toSykInnPasient(): SykInnPasient =
    SykInnPasient(
        fornavn = navn?.fornavn.orEmpty(),
        mellomnavn = navn?.mellomnavn,
        etternavn = navn?.etternavn.orEmpty(),
        ident = fnr,
    )

private fun Sykmelding.toSykInnBehandler(): SykInnBehandler =
    when (this) {
        is DigitalSykmelding ->
            behandler.toSykInnBehandler(sykmelder.helsepersonellKategori.toShortCode())
        is XmlSykmelding ->
            behandler.toSykInnBehandler(sykmelder.helsepersonellKategori.toShortCode())
        is Papirsykmelding ->
            behandler.toSykInnBehandler(sykmelder.helsepersonellKategori.toShortCode())
        is UtenlandskSykmelding -> throw IllegalStateException("these are not supported")
    }

private fun Behandler.toSykInnBehandler(helsepersonellShortCode: String): SykInnBehandler =
    SykInnBehandler(
        fornavn = navn.fornavn,
        mellomnavn = navn.mellomnavn,
        etternavn = navn.etternavn,
        hpr = ids.single { it.type == PersonIdType.HPR }.id,
        fnr = ids.single { it.type == PersonIdType.FNR }.id,
        helsepersonellkategori = listOf(helsepersonellShortCode),
    )

private fun Sykmelding.toLegekontorTlf(): String? =
    when (this) {
        is DigitalSykmelding -> behandler.firstTlf()
        is XmlSykmelding -> behandler.firstTlf()
        is Papirsykmelding -> behandler.firstTlf()
        is UtenlandskSykmelding -> null
    }

private fun Behandler.firstTlf(): String? =
    kontaktinfo.firstOrNull { it.type == KontaktinfoType.TLF }?.value

private fun getOrgNr(sender: Organisasjon): String? {
    return sender.underOrganisasjon?.ids?.firstOrNull { it.type == OrgIdType.ENH }?.id
        ?: sender.ids.firstOrNull { it.type == OrgIdType.ENH }?.id
}

private fun HelsepersonellKategori.toShortCode(): String =
    when (this) {
        HelsepersonellKategori.HELSESEKRETAR -> "HE"
        HelsepersonellKategori.KIROPRAKTOR -> "KI"
        HelsepersonellKategori.LEGE -> "LE"
        HelsepersonellKategori.MANUELLTERAPEUT -> "MT"
        HelsepersonellKategori.TANNLEGE -> "TL"
        HelsepersonellKategori.TANNHELSESEKRETAR -> "TH"
        HelsepersonellKategori.FYSIOTERAPEUT -> "FT"
        HelsepersonellKategori.SYKEPLEIER -> "SP"
        HelsepersonellKategori.HJELPEPLEIER -> "HP"
        HelsepersonellKategori.HELSEFAGARBEIDER -> "HF"
        HelsepersonellKategori.JORDMOR -> "JO"
        HelsepersonellKategori.AUDIOGRAF -> "AU"
        HelsepersonellKategori.NAPRAPAT -> "NP"
        HelsepersonellKategori.PSYKOLOG -> "PS"
        HelsepersonellKategori.FOTTERAPEUT -> "FO"
        HelsepersonellKategori.AMBULANSEARBEIDER -> "AA"
        HelsepersonellKategori.USPESIFISERT -> "XX"
        HelsepersonellKategori.UGYLDIG -> "HS"
        HelsepersonellKategori.IKKE_OPPGITT -> ""
    }

private fun Aktivitet.toSykInnAktivitet(): SykInnAktivitet {
    return when (this) {
        is AktivitetIkkeMulig ->
            SykInnAktivitet.IkkeMulig(
                fom = this.fom,
                tom = this.tom,
                arbeidsrelatertArsak =
                    this.arbeidsrelatertArsak?.let { arsak ->
                        SykInnArbeidsrelatertArsak(
                            arbeidsrelaterteArsaker = arsak.arsak,
                            annenArbeidsrelatertArsak = arsak.beskrivelse,
                        )
                    },
            )

        is Gradert ->
            SykInnAktivitet.Gradert(
                fom = this.fom,
                tom = this.tom,
                grad = this.grad,
                reisetilskudd = this.reisetilskudd,
            )
        is Avventende ->
            SykInnAktivitet.Avventende(
                fom = this.fom,
                tom = this.tom,
                innspillTilArbeidsgiver = this.innspillTilArbeidsgiver,
            )
        is Behandlingsdager ->
            SykInnAktivitet.Behandlingsdager(
                fom = this.fom,
                tom = this.tom,
                antallBehandlingsdager = this.antallBehandlingsdager,
            )
        is Reisetilskudd -> SykInnAktivitet.Reisetilskudd(fom = this.fom, tom = this.tom)
    }
}

private fun SykmeldingRecord.toSykmeldingValues(): SykInnSykmeldingValues {
    return SykInnSykmeldingValues(
        pasientenSkalSkjermes = sykmelding.medisinskVurdering.skjermetForPasient,
        hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose?.toSykInnDiagnoseInfo(),
        bidiagnoser =
            (sykmelding.medisinskVurdering.biDiagnoser ?: emptyList()).map {
                it.toSykInnDiagnoseInfo()
            },
        aktivitet = sykmelding.aktivitet.map { it.toSykInnAktivitet() },
        svangerskapsrelatert = sykmelding.medisinskVurdering.svangerskap,
        meldinger = toMeldinger(meldingTilArbeidsgiver(), meldingTilNav()),
        yrkesskade = sykmelding.medisinskVurdering.yrkesskade?.toSykInnYrkesskade(),
        arbeidsgiver = sykmelding.toArbeidsgiver(),
        tilbakedatering = toTilbakedatering(kontaktDato(), tilbakedatertBegrunnelse()),
        utdypendeSporsmal = sykmelding.toUtdypendeSpm(),
        annenFravarsgrunn = sykmelding.toSykInnFravarsGrunn(),
    )
}

private fun Sykmelding.toUtdypendeSpm(): SykInnUtdypendeSporsmal? {
    return when (this) {
        is UtenlandskSykmelding -> null
        is DigitalSykmelding -> this.utdypendeSporsmal?.toSykInnUtdypendeSpm()
        is Papirsykmelding -> this.utdypendeOpplysninger?.toSykInnUtdypendeSpm()
        is XmlSykmelding -> this.utdypendeOpplysninger?.toSykInnUtdypendeSpm()
    }
}

private fun List<UtdypendeSporsmal>.toSykInnUtdypendeSpm(): SykInnUtdypendeSporsmal? {
    if (isNullOrEmpty()) return null

    return SykInnUtdypendeSporsmal(
        hensynPaArbeidsplassen = tryGetSpm(HENSYN_PA_ARBEIDSPLASSEN),
        medisinskOppsummering = tryGetSpm(MEDISINSK_OPPSUMMERING),
        utfordringerMedGradertArbeid = tryGetSpm(UTFORDRINGER_MED_GRADERT_ARBEID),
        utfordringerMedArbeid = tryGetSpm(UTFORDRINGER_MED_ARBEID),
        behandlingOgFremtidigArbeid = tryGetSpm(BEHANDLING_OG_FREMTIDIG_ARBEID),
        uavklarteForhold = tryGetSpm(UAVKLARTE_FORHOLD),
        forventetHelsetilstandUtvikling = tryGetSpm(FORVENTET_HELSETILSTAND_UTVIKLING),
        medisinskeHensyn = tryGetSpm(MEDISINSKE_HENSYN),
    )
}

private fun List<UtdypendeSporsmal>.tryGetSpm(
    supportedSpmType: SupportedSpmType
): SykInnUtdypendeSporsmalSvar? = singleOrNull { it.type.name == supportedSpmType.name }?.toSpm()

private fun UtdypendeSporsmal.toSpm(): SykInnUtdypendeSporsmalSvar {
    return SykInnUtdypendeSporsmalSvar(
        sporsmalstekst = sporsmal ?: SupportedSpmType.valueOf(type.name).defaultSpm,
        svar = svar,
    )
}

private enum class SupportedSpmType(vararg val keys: String, val defaultSpm: String) {
    MEDISINSK_OPPSUMMERING(
        "6.3.1",
        "6.4.1",
        "6.5.1",
        defaultSpm =
            "Gi en kort medisinsk oppsummering av tilstanden (sykehistorie, hovedsymptomer, behandling)",
    ),
    UTFORDRINGER_MED_GRADERT_ARBEID(
        "6.3.2",
        defaultSpm =
            "Beskriv kort hvilke helsemessige begrensninger som gjør det vanskelig å jobbe gradert",
    ),
    HENSYN_PA_ARBEIDSPLASSEN(
        "6.3.3",
        defaultSpm =
            "Beskriv eventuelle medisinske forhold som bør ivaretas ved eventuell tilbakeføring til nåværende arbeid (ikke obligatorisk)",
    ),
    UTFORDRINGER_MED_ARBEID(
        "6.4.2",
        "6.5.2",
        defaultSpm =
            "Beskriv kort hvilke utfordringer helsetilstanden gir i arbeidssituasjonen nå. Oppgi også kort hva pasienten likevel kan mestre",
    ),
    BEHANDLING_OG_FREMTIDIG_ARBEID(
        "6.4.3",
        defaultSpm =
            "Beskriv pågående og planlagt utredning/behandling, og om dette forventes å påvirke muligheten for økt arbeidsdeltakelse fremover",
    ),
    UAVKLARTE_FORHOLD(
        "6.4.4",
        defaultSpm =
            "Er det forhold som fortsatt er uavklarte eller hindrer videre arbeidsdeltakelse, som Nav bør være kjent med i sin oppfølging?",
    ),
    FORVENTET_HELSETILSTAND_UTVIKLING(
        "6.5.3",
        defaultSpm =
            "Hvordan forventes helsetilstanden å utvikle seg de neste 3-6 månedene med tanke på mulighet for økt arbeidsdeltakelse?",
    ),
    MEDISINSKE_HENSYN(
        "6.5.4",
        defaultSpm =
            "Er det medisinske hensyn eller avklaringsbehov Nav bør kjenne til i videre oppfølging?",
    );

    fun isType(string: String): Boolean {
        return keys.contains(string)
    }
}

private fun Map<String, Map<String, SporsmalSvar>>.toSykInnUtdypendeSpm():
    SykInnUtdypendeSporsmal? {
    val sporsmal =
        values.flatMap { it.entries }.filter { isSupported(it.key) }.sortedByDescending { it.key }

    if (sporsmal.isEmpty()) return null

    return SykInnUtdypendeSporsmal(
        hensynPaArbeidsplassen = sporsmal.tryMapSpm(HENSYN_PA_ARBEIDSPLASSEN),
        medisinskOppsummering = sporsmal.tryMapSpm(MEDISINSK_OPPSUMMERING),
        utfordringerMedGradertArbeid = sporsmal.tryMapSpm(UTFORDRINGER_MED_GRADERT_ARBEID),
        utfordringerMedArbeid = sporsmal.tryMapSpm(UTFORDRINGER_MED_ARBEID),
        behandlingOgFremtidigArbeid = sporsmal.tryMapSpm(BEHANDLING_OG_FREMTIDIG_ARBEID),
        uavklarteForhold = sporsmal.tryMapSpm(UAVKLARTE_FORHOLD),
        forventetHelsetilstandUtvikling = sporsmal.tryMapSpm(FORVENTET_HELSETILSTAND_UTVIKLING),
        medisinskeHensyn = sporsmal.tryMapSpm(MEDISINSKE_HENSYN),
    )
}

private fun List<Map.Entry<String, SporsmalSvar>>.tryMapSpm(
    supportedSpm: SupportedSpmType
): SykInnUtdypendeSporsmalSvar? {
    return firstOrNull { supportedSpm.isType(it.key) }
        ?.let {
            SykInnUtdypendeSporsmalSvar(
                sporsmalstekst = it.value.sporsmal ?: supportedSpm.defaultSpm,
                svar = it.value.svar,
            )
        }
}

private fun isSupported(spmKey: String): Boolean {
    return when (spmKey) {
        "6.3.1",
        "6.3.2",
        "6.3.3" -> true
        "6.4.1",
        "6.4.2",
        "6.4.3",
        "6.4.4" -> true
        "6.5.1",
        "6.5.2",
        "6.5.3",
        "6.5.4" -> true
        else -> {
            log.debug("$spmKey is not supported")
            false
        }
    }
}

private fun Sykmelding.toSykInnFravarsGrunn(): AnnenFravarsgrunn? {
    return when (val vurdering = this.medisinskVurdering) {
        is DigitalMedisinskVurdering -> vurdering.annenFravarsgrunn
        is LegacyMedisinskVurdering -> vurdering.annenFraversArsak?.arsak?.firstOrNull()
    }
}

private fun SykmeldingRecord.toTilbakedatering(
    kontaktDato: LocalDate?,
    tilbakedatertBegrunnelse: String?,
): SykInnTilbakedatering? {
    if (kontaktDato == null && tilbakedatertBegrunnelse == null) return null

    return SykInnTilbakedatering(kontaktDato, tilbakedatertBegrunnelse)
}

private fun SykmeldingRecord.kontaktDato(): LocalDate? {
    return when (val sykmelding = sykmelding) {
        is DigitalSykmelding -> sykmelding.tilbakedatering?.kontaktDato
        is Papirsykmelding -> sykmelding.tilbakedatering?.kontaktDato
        is XmlSykmelding -> sykmelding.tilbakedatering?.kontaktDato
        is UtenlandskSykmelding -> null
    }
}

private fun SykmeldingRecord.tilbakedatertBegrunnelse(): String? {
    return when (val sykmelding = sykmelding) {
        is DigitalSykmelding -> sykmelding.tilbakedatering?.begrunnelse
        is Papirsykmelding -> sykmelding.tilbakedatering?.begrunnelse
        is XmlSykmelding -> sykmelding.tilbakedatering?.begrunnelse
        is UtenlandskSykmelding -> null
    }
}

private fun Sykmelding.toArbeidsgiver(): SykInnArbeidsgiver? {
    return when (this) {
        is Papirsykmelding -> this.arbeidsgiver.toSykInnArbeidsgiver()
        is DigitalSykmelding -> this.arbeidsgiver.toSykInnArbeidsgiver()
        is XmlSykmelding -> this.arbeidsgiver.toSykInnArbeidsgiver()
        is UtenlandskSykmelding -> null
    }
}

private fun ArbeidsgiverInfo.toSykInnArbeidsgiver(): SykInnArbeidsgiver? {
    return when (this) {
        is EnArbeidsgiver -> SykInnArbeidsgiver(false, this.navn)
        is FlereArbeidsgivere -> SykInnArbeidsgiver(true, this.navn)
        is IngenArbeidsgiver -> null
    }
}

private fun Yrkesskade.toSykInnYrkesskade(): SykInnYrkesskade {
    return SykInnYrkesskade(true, this.yrkesskadeDato)
}

private fun SykmeldingRecord.meldingTilNav(): String? {
    return when (val sykmelding = sykmelding) {
        is UtenlandskSykmelding -> null
        is DigitalSykmelding -> sykmelding.bistandNav?.beskrivBistand
        is Papirsykmelding -> sykmelding.bistandNav?.beskrivBistand
        is XmlSykmelding -> sykmelding.bistandNav?.beskrivBistand
    }
}

private fun SykmeldingRecord.meldingTilArbeidsgiver(): String? {
    return when (val sykmelding = sykmelding) {
        is UtenlandskSykmelding -> null
        is DigitalSykmelding -> getMeldingTilArbeidsgiver(sykmelding.arbeidsgiver)
        is Papirsykmelding -> getMeldingTilArbeidsgiver(sykmelding.arbeidsgiver)
        is XmlSykmelding -> getMeldingTilArbeidsgiver(sykmelding.arbeidsgiver)
    }
}

private fun getMeldingTilArbeidsgiver(arbeidsgiver: ArbeidsgiverInfo): String? =
    when (arbeidsgiver) {
        is EnArbeidsgiver -> arbeidsgiver.meldingTilArbeidsgiver
        is FlereArbeidsgivere -> arbeidsgiver.meldingTilArbeidsgiver
        is IngenArbeidsgiver -> null
    }

private fun SykmeldingRecord.toMeldinger(
    meldingTilArbeidsgiver: String?,
    meldingTilNav: String?,
): SykInnMeldinger? {
    if (meldingTilArbeidsgiver == null && meldingTilNav == null) return null

    return SykInnMeldinger(tilNav = meldingTilNav, tilArbeidsgiver = meldingTilArbeidsgiver)
}

private fun DiagnoseInfo.toSykInnDiagnoseInfo(): SykInnDiagnoseInfo =
    SykInnDiagnoseInfo(
        system =
            when (this.system) {
                DiagnoseSystem.ICPC2 -> SykInnDiagnoseSystem.ICPC2
                DiagnoseSystem.ICD10 -> SykInnDiagnoseSystem.ICD10
                DiagnoseSystem.ICPC2B -> SykInnDiagnoseSystem.ICPC2B
                else -> {
                    throw IllegalArgumentException("Unsupported system: ${this.system}")
                }
            },
        code = kode,
    )
