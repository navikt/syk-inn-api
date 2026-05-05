package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import java.util.*
import no.nav.tsm.core.common.SykInnDiagnoseSystem
import no.nav.tsm.core.logger
import no.nav.tsm.modules.sykmeldinger.domain.*
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingMeta.*
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SupportedSpmType.*
import no.nav.tsm.sykmelding.input.core.model.*
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.metadata.*

private val log = logger()

fun RecordWithResources.toVerifiedSykmelding(): VerifiedSykInnSykmelding {
    return VerifiedSykInnSykmelding(
        sykmeldingId = UUID.fromString(record.sykmelding.id),
        values = record.toSykmeldingValues(),
        meta = toMetadata(),
        result = record.validation.toResult(),
        type =
            when (this.record.sykmelding.type) {
                SykmeldingType.DIGITAL -> SykInnSykmeldingType.DIGITAL
                SykmeldingType.XML -> SykInnSykmeldingType.XML
                SykmeldingType.PAPIR -> SykInnSykmeldingType.PAPIR
                SykmeldingType.UTENLANDSK -> SykInnSykmeldingType.UTENLANDSK
            },
    )
}

private fun ValidationResult.toResult(): SykInnSykmeldingRuleResult =
    when (this.status) {
        RuleType.OK -> SykInnSykmeldingRuleResult.OK
        RuleType.PENDING,
        RuleType.INVALID -> {
            when (
                val rule =
                    this.rules.sortedByDescending { it.timestamp }.firstOrNull { it !is Rule.OK }
            ) {
                null ->
                    throw IllegalStateException(
                        "ValidationResult status=$status but no non-OK rule present"
                    )

                is Rule.Invalid ->
                    SykInnSykmeldingRuleResult.Outcome(
                        type = status,
                        rule = rule.name,
                        message = rule.reason.sykmelder,
                    )

                is Rule.Pending ->
                    SykInnSykmeldingRuleResult.Outcome(
                        type = status,
                        rule = rule.name,
                        message = rule.reason.sykmelder,
                    )

                is Rule.OK -> error("unreachable: filtered above")
            }
        }
    }

private fun RecordWithResources.toMetadata(): SykInnSykmeldingMeta {
    val sykmeldingRecord = this.record

    val source = record.sykmelding.metadata.avsenderSystem.navn
    val mottattDato = record.sykmelding.metadata.mottattDato
    val pasient = record.sykmelding.pasient.toSykInnPasient()

    if (sykmeldingRecord is SykmeldingRecord.Utenlandsk) {
        return Utenlandsk(source = source, mottatt = mottattDato, pasient = pasient)
    }

    val sykmelder =
        when (sykmeldingRecord) {
            is SykmeldingRecord.Digital -> sykmeldingRecord.sykmelding.sykmelder
            is SykmeldingRecord.Papir -> sykmeldingRecord.sykmelding.sykmelder
            is SykmeldingRecord.Xml -> sykmeldingRecord.sykmelding.sykmelder
        }

    require(this is RecordWithResources.Nasjonal) {
        "Resource meta cannot be Utenlandsk at this point, id: ${this.record.sykmelding.id}"
    }

    val behandler =
        SykInnBehandler(
            fornavn = this.navn.fornavn,
            mellomnavn = this.navn.mellomnavn,
            etternavn = this.navn.etternavn,
            hpr = this.hpr,
            ident = this.ident,
            helsepersonellkategori = listOf(sykmelder.helsepersonellKategori.toShortCode()),
        )

    return when (sykmeldingRecord) {
        is SykmeldingRecord.Digital ->
            Digital(
                source = source,
                mottatt = mottattDato,
                pasient = pasient,
                behandler = behandler,
                legekontorOrgnr = sykmeldingRecord.metadata.orgnummer,
                legekontorTlf =
                    sykmeldingRecord.sykmelding.behandler.kontaktinfo.getTelefonnummer()
                        ?: error(
                            "Sykmelding of type DIGITAL with id ${sykmeldingRecord.sykmelding.id} is missing telefonnummer directly on behandler"
                        ),
            )

        is SykmeldingRecord.Xml ->
            Legacy(
                source = source,
                mottatt = mottattDato,
                pasient = pasient,
                behandler = behandler,
                legekontorOrgnr = sykmeldingRecord.metadata.getOrgNr(),
                legekontorTlf =
                    sykmeldingRecord.metadata.getOrgTlf()
                        ?: sykmeldingRecord.sykmelding.behandler.kontaktinfo.getTelefonnummer(),
            )

        is SykmeldingRecord.Papir ->
            Legacy(
                source = source,
                mottatt = mottattDato,
                pasient = pasient,
                behandler = behandler,
                legekontorOrgnr = sykmeldingRecord.metadata.sender.getOrgNr(),
                legekontorTlf =
                    sykmeldingRecord.metadata.sender.getOrgTlf()
                        ?: sykmeldingRecord.sykmelding.behandler.kontaktinfo.getTelefonnummer(),
            )
    }
}

private fun Pasient.toSykInnPasient(): SykInnPasient =
    SykInnPasient(
        fornavn = navn?.fornavn.orEmpty(),
        mellomnavn = navn?.mellomnavn,
        etternavn = navn?.etternavn.orEmpty(),
        ident = fnr,
    )

private fun MessageMetadata.Xml.getOrgNr(): String? {
    if (this is MessageMetadata.Xml.Egenmeldt) return null

    return when (this) {
        is MessageMetadata.Xml.Emottak.EDI -> this.sender
        is MessageMetadata.Xml.Emottak.Legacy -> this.sender
    }.getOrgNr()
}

private fun Organisasjon.getOrgNr(): String? =
    underOrganisasjon?.ids?.firstOrNull { it.type == OrgIdType.ENH }?.id
        ?: ids.firstOrNull { it.type == OrgIdType.ENH }?.id

private fun MessageMetadata.Xml.getOrgTlf(): String? {
    if (this is MessageMetadata.Xml.Egenmeldt) return null

    return when (this) {
        is MessageMetadata.Xml.Emottak.EDI -> this.sender
        is MessageMetadata.Xml.Emottak.Legacy -> this.sender
    }.getOrgTlf()
}

private fun Organisasjon.getOrgTlf(): String? =
    this.underOrganisasjon?.kontaktinfo?.getTelefonnummer() ?: this.kontaktinfo?.getTelefonnummer()

private fun List<Kontaktinfo>.getTelefonnummer(): String? =
    this.filterNot {
            listOf(KontaktinfoType.IKKE_OPPGITT, KontaktinfoType.FAX_TELEFAKS).contains(it.type)
        }
        .firstOrNull()
        ?.value

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
        is Aktivitet.IkkeMulig ->
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

        is Aktivitet.Gradert ->
            SykInnAktivitet.Gradert(
                fom = this.fom,
                tom = this.tom,
                grad = this.grad,
                reisetilskudd = this.reisetilskudd,
            )

        is Aktivitet.Avventende ->
            SykInnAktivitet.Avventende(
                fom = this.fom,
                tom = this.tom,
                innspillTilArbeidsgiver = this.innspillTilArbeidsgiver,
            )

        is Aktivitet.Behandlingsdager ->
            SykInnAktivitet.Behandlingsdager(
                fom = this.fom,
                tom = this.tom,
                antallBehandlingsdager = this.antallBehandlingsdager,
            )

        is Aktivitet.Reisetilskudd -> SykInnAktivitet.Reisetilskudd(fom = this.fom, tom = this.tom)
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
        meldinger = sykmelding.toMeldinger(),
        yrkesskade = sykmelding.medisinskVurdering.yrkesskade?.toSykInnYrkesskade(),
        arbeidsgiver = sykmelding.toArbeidsgiver(),
        tilbakedatering = sykmelding.toTilbakedatering(),
        utdypendeSporsmal = sykmelding.toUtdypendeSpm(),
        annenFravarsgrunn = sykmelding.medisinskVurdering.toSykInnFravarsGrunn(),
    )
}

private fun Sykmelding.toTilbakedatering(): SykInnTilbakedatering? {
    when (this) {
        is Sykmelding.Nasjonal -> {
            val kontaktDato = this.tilbakedatering?.kontaktDato
            val begrunnelse = this.tilbakedatering?.begrunnelse
            if (kontaktDato == null && begrunnelse == null) {
                return null
            }
            return SykInnTilbakedatering(kontaktdato = kontaktDato, begrunnelse = begrunnelse)
        }

        else -> return null
    }
}

private fun Sykmelding.toMeldinger(): SykInnMeldinger? {
    when (this) {
        is Sykmelding.Nasjonal -> {
            val meldingTilArbeidsgiver =
                when (val arbeidsgiver = this.arbeidsgiver) {
                    is ArbeidsgiverInfo.En -> arbeidsgiver.meldingTilArbeidsgiver
                    is ArbeidsgiverInfo.Flere -> arbeidsgiver.meldingTilArbeidsgiver
                    is ArbeidsgiverInfo.Ingen -> null
                }

            val meldingTilNav = this.bistandNav?.beskrivBistand

            if (meldingTilNav == null && meldingTilArbeidsgiver == null) {
                return null
            }

            return SykInnMeldinger(tilNav = meldingTilNav, tilArbeidsgiver = meldingTilArbeidsgiver)
        }

        else -> return null
    }
}

private fun Sykmelding.toUtdypendeSpm(): SykInnUtdypendeSporsmal? {
    return when (this) {
        is Sykmelding.Utenlandsk -> null
        is Sykmelding.Digital -> this.utdypendeSporsmal?.toSykInnUtdypendeSpm()
        is Sykmelding.Nasjonal.Legacy -> this.utdypendeOpplysninger?.toSykInnUtdypendeSpm()
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
        sporsmalstekst = sporsmal ?: valueOf(type.name).defaultSpm,
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

private fun MedisinskVurdering.toSykInnFravarsGrunn(): AnnenFravarsgrunn? =
    when (this) {
        is MedisinskVurdering.Digital -> this.annenFravarsgrunn
        is MedisinskVurdering.Legacy -> this.annenFraversArsak?.arsak?.firstOrNull()
    }

private fun Sykmelding.toArbeidsgiver(): SykInnArbeidsgiver? {
    return when (this) {
        is Sykmelding.Nasjonal -> this.arbeidsgiver.toSykInnArbeidsgiver()
        is Sykmelding.Utenlandsk -> null
    }
}

private fun ArbeidsgiverInfo.toSykInnArbeidsgiver(): SykInnArbeidsgiver? {
    return when (this) {
        is ArbeidsgiverInfo.En -> SykInnArbeidsgiver(false, this.navn)
        is ArbeidsgiverInfo.Flere -> SykInnArbeidsgiver(true, this.navn)
        is ArbeidsgiverInfo.Ingen -> null
    }
}

private fun Yrkesskade.toSykInnYrkesskade(): SykInnYrkesskade {
    return SykInnYrkesskade(true, this.yrkesskadeDato)
}

private fun DiagnoseInfo.toSykInnDiagnoseInfo(): SykInnDiagnoseInfo =
    SykInnDiagnoseInfo.tryParse(
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
        fallbackText = this.tekst,
    )
