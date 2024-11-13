package no.nav.tsm.sykinnapi.mapper

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import no.nav.helse.diagnosekoder.Diagnosekoder
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.msgHead.XMLCS
import no.nav.helse.msgHead.XMLCV
import no.nav.helse.msgHead.XMLDocument
import no.nav.helse.msgHead.XMLHealthcareProfessional
import no.nav.helse.msgHead.XMLIdent
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLMsgInfo
import no.nav.helse.msgHead.XMLOrganisation
import no.nav.helse.msgHead.XMLReceiver
import no.nav.helse.msgHead.XMLRefDoc
import no.nav.helse.msgHead.XMLSender
import no.nav.helse.sm2013.Address
import no.nav.helse.sm2013.CS
import no.nav.helse.sm2013.CV
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.helse.sm2013.Ident
import no.nav.helse.sm2013.NavnType
import no.nav.helse.sm2013.TeleCom
import no.nav.helse.sm2013.URL
import no.nav.tsm.sykinnapi.modell.Aktivitet as SykInAktivitet
import no.nav.tsm.sykinnapi.modell.Hoveddiagnose

fun mapToFellesformat(
    sykmelderHpr: String,
    sykmeldingId: String,
    pasientfnr: String,
    hoveddiagnose: Hoveddiagnose,
    sykInnAktivitet: SykInAktivitet,
    now: LocalDateTime
): XMLEIFellesformat {

    return XMLEIFellesformat().apply {
        any.add(
            XMLMsgHead().apply {
                msgInfo =
                    XMLMsgInfo().apply {
                        type =
                            XMLCS().apply {
                                dn = "Medisinsk vurdering av arbeidsmulighet ved sykdom, sykmelding"
                                v = "SYKMELD"
                            }
                        miGversion = "v1.2 2006-05-24"
                        genDate = now.toString()
                        msgId = sykmeldingId
                        ack =
                            XMLCS().apply {
                                dn = "Ja"
                                v = "J"
                            }
                        sender =
                            XMLSender().apply {
                                comMethod =
                                    XMLCS().apply {
                                        dn = "EDI"
                                        v = "EDI"
                                    }
                                organisation =
                                    XMLOrganisation().apply {
                                        healthcareProfessional =
                                            XMLHealthcareProfessional().apply {
                                                givenName = ""
                                                middleName = ""
                                                familyName = ""
                                                ident.addAll(
                                                    listOf(
                                                        XMLIdent().apply {
                                                            id = sykmelderHpr
                                                            typeId =
                                                                XMLCV().apply {
                                                                    dn = "HPR-nummer"
                                                                    s = "2.16.578.1.12.4.1.1.8116"
                                                                    v = "HPR"
                                                                }
                                                        },
                                                        XMLIdent().apply {
                                                            id = sykmelderHpr
                                                            typeId =
                                                                XMLCV().apply {
                                                                    dn = "Fødselsnummer"
                                                                    s = "2.16.578.1.12.4.1.1.8116"
                                                                    v = "FNR"
                                                                }
                                                        },
                                                    ),
                                                )
                                            }
                                    }
                            }
                        receiver =
                            XMLReceiver().apply {
                                comMethod =
                                    XMLCS().apply {
                                        dn = "EDI"
                                        v = "EDI"
                                    }
                                organisation =
                                    XMLOrganisation().apply {
                                        organisationName = "NAV"
                                        ident.addAll(
                                            listOf(
                                                XMLIdent().apply {
                                                    id = "79768"
                                                    typeId =
                                                        XMLCV().apply {
                                                            dn =
                                                                "Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"
                                                            s = "2.16.578.1.12.4.1.1.9051"
                                                            v = "HER"
                                                        }
                                                },
                                                XMLIdent().apply {
                                                    id = "889640782"
                                                    typeId =
                                                        XMLCV().apply {
                                                            dn =
                                                                "Organisasjonsnummeret i Enhetsregister (Brønøysund)"
                                                            s = "2.16.578.1.12.4.1.1.9051"
                                                            v = "ENH"
                                                        }
                                                },
                                            ),
                                        )
                                    }
                            }
                    }
                document.add(
                    XMLDocument().apply {
                        refDoc =
                            XMLRefDoc().apply {
                                msgType =
                                    XMLCS().apply {
                                        dn = "XML-instans"
                                        v = "XML"
                                    }
                                content =
                                    XMLRefDoc.Content().apply {
                                        any.add(
                                            HelseOpplysningerArbeidsuforhet().apply {
                                                syketilfelleStartDato = now.toLocalDate()
                                                pasient =
                                                    HelseOpplysningerArbeidsuforhet.Pasient()
                                                        .apply {
                                                            navn =
                                                                NavnType().apply {
                                                                    fornavn = ""
                                                                    mellomnavn = ""
                                                                    etternavn = ""
                                                                }
                                                            fodselsnummer =
                                                                Ident().apply {
                                                                    id = pasientfnr
                                                                    typeId =
                                                                        CV().apply {
                                                                            dn = "Fødselsnummer"
                                                                            s =
                                                                                "2.16.578.1.12.4.1.1.8116"
                                                                            v = "FNR"
                                                                        }
                                                                }
                                                        }
                                                arbeidsgiver = tilArbeidsgiver()
                                                medisinskVurdering =
                                                    tilMedisinskVurdering(
                                                        hoveddiagnose,
                                                    )
                                                aktivitet =
                                                    HelseOpplysningerArbeidsuforhet.Aktivitet()
                                                        .apply {
                                                            periode.addAll(
                                                                tilPeriodeListe(
                                                                    sykInnAktivitet,
                                                                ),
                                                            )
                                                        }
                                                prognose = null
                                                utdypendeOpplysninger = null
                                                tiltak = null
                                                meldingTilNav = null
                                                meldingTilArbeidsgiver = null
                                                kontaktMedPasient =
                                                    HelseOpplysningerArbeidsuforhet
                                                        .KontaktMedPasient()
                                                        .apply {
                                                            kontaktDato = null
                                                            begrunnIkkeKontakt = null
                                                            behandletDato = now
                                                        }
                                                behandler = tilBehandler(sykmelderHpr)
                                                avsenderSystem =
                                                    HelseOpplysningerArbeidsuforhet.AvsenderSystem()
                                                        .apply {
                                                            systemNavn = "syk-inn"
                                                            systemVersjon = "1.0.0"
                                                        }
                                                strekkode = "123456789qwerty"
                                            },
                                        )
                                    }
                            }
                    },
                )
            },
        )
    }
}

fun tilBehandler(sykmelderHpr: String): HelseOpplysningerArbeidsuforhet.Behandler =
    HelseOpplysningerArbeidsuforhet.Behandler().apply {
        navn =
            NavnType().apply {
                fornavn = ""
                mellomnavn = ""
                etternavn = ""
            }
        id.addAll(
            listOf(
                Ident().apply {
                    id = sykmelderHpr
                    typeId =
                        CV().apply {
                            dn = "HPR-nummer"
                            s = "6.87.654.3.21.9.8.7.6543.2198"
                            v = "HPR"
                        }
                },
            ),
        )
        adresse = Address()
        kontaktInfo.add(
            TeleCom().apply {
                typeTelecom =
                    CS().apply {
                        v = "HP"
                        dn = "Hovedtelefon"
                    }
                teleAddress = URL().apply { v = "" }
            },
        )
    }

fun tilPeriodeListe(
    aktivitet: SykInAktivitet
): List<HelseOpplysningerArbeidsuforhet.Aktivitet.Periode> {
    val periodeListe = ArrayList<HelseOpplysningerArbeidsuforhet.Aktivitet.Periode>()

    when (aktivitet) {
        is SykInAktivitet.Gradert -> {
            periodeListe.add(
                HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                    periodeFOMDato = toLocalDate(aktivitet.fom)
                    periodeTOMDato = toLocalDate(aktivitet.tom)
                    aktivitetIkkeMulig = null
                    avventendeSykmelding = null
                    gradertSykmelding =
                        HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.GradertSykmelding()
                            .apply {
                                sykmeldingsgrad = aktivitet.grad
                                isReisetilskudd = false
                            }
                    behandlingsdager = null
                    isReisetilskudd = false
                },
            )
        }
        is SykInAktivitet.AktivitetIkkeMulig -> {
            periodeListe.add(
                HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
                    periodeFOMDato = toLocalDate(aktivitet.fom)
                    periodeTOMDato = toLocalDate(aktivitet.tom)
                    aktivitetIkkeMulig =
                        HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AktivitetIkkeMulig()
                            .apply {
                                medisinskeArsaker = null
                                arbeidsplassen = null
                                avventendeSykmelding = null
                                gradertSykmelding = null
                                behandlingsdager = null
                                isReisetilskudd = false
                            }
                },
            )
        }
    }

    return periodeListe
}

private fun toLocalDate(dateString: String): LocalDate {
    val formatterDateIso = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.of("nb", "NO"))
    return LocalDate.parse(dateString, formatterDateIso)
}

fun tilArbeidsgiver(): HelseOpplysningerArbeidsuforhet.Arbeidsgiver =
    HelseOpplysningerArbeidsuforhet.Arbeidsgiver().apply {
        harArbeidsgiver =
            CS().apply {
                dn = "Ein arbeidsgiver"
                v = "1"
            }

        navnArbeidsgiver = ""
        yrkesbetegnelse = ""
        stillingsprosent = 0
    }

fun tilMedisinskVurdering(
    hoveddiagnose: Hoveddiagnose,
): HelseOpplysningerArbeidsuforhet.MedisinskVurdering {

    return HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
        hovedDiagnose =
            HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode =
                    toMedisinskVurderingDiagnose(
                        hoveddiagnose.code,
                        hoveddiagnose.system.toString(),
                        hoveddiagnose.code,
                    )
            }

        isSkjermesForPasient = false
        annenFraversArsak = null
        isSvangerskap = false
        isYrkesskade = false
        yrkesskadeDato = null
    }
}

fun identifiserDiagnoseKodeverk(diagnoseKode: String, system: String?, diagnose: String?): String {
    val sanitisertSystem = system?.replace(".", "")?.replace(" ", "")?.replace("-", "")?.uppercase()
    val sanitisertKode = diagnoseKode.replace(".", "").replace(" ", "").uppercase()

    return if (sanitisertSystem == "ICD10") {
        Diagnosekoder.ICD10_CODE
    } else if (sanitisertSystem == "ICPC2") {
        Diagnosekoder.ICPC2_CODE
    } else if (
        Diagnosekoder.icd10.containsKey(sanitisertKode) &&
            Diagnosekoder.icd10[sanitisertKode]?.text == diagnose
    ) {
        Diagnosekoder.ICD10_CODE
    } else if (
        Diagnosekoder.icpc2.containsKey(sanitisertKode) &&
            Diagnosekoder.icpc2[sanitisertKode]?.text == diagnose
    ) {
        Diagnosekoder.ICPC2_CODE
    } else {
        ""
    }
}

fun toMedisinskVurderingDiagnose(
    originalDiagnosekode: String,
    originalSystem: String?,
    diagnose: String?,
): CV {
    val diagnosekode =
        if (originalDiagnosekode.contains(".")) {
            originalDiagnosekode.replace(".", "").uppercase().replace(" ", "")
        } else {
            originalDiagnosekode.uppercase().replace(" ", "")
        }

    val identifisertKodeverk =
        identifiserDiagnoseKodeverk(originalDiagnosekode, originalSystem, diagnose)

    when {
        identifisertKodeverk == Diagnosekoder.ICD10_CODE &&
            Diagnosekoder.icd10.containsKey(diagnosekode) -> {
            return CV().apply {
                s = Diagnosekoder.ICD10_CODE
                v = diagnosekode
                dn = Diagnosekoder.icd10[diagnosekode]?.text ?: ""
            }
        }
        identifisertKodeverk == Diagnosekoder.ICPC2_CODE &&
            Diagnosekoder.icpc2.containsKey(diagnosekode) -> {
            return CV().apply {
                s = Diagnosekoder.ICPC2_CODE
                v = diagnosekode
                dn = Diagnosekoder.icpc2[diagnosekode]?.text ?: ""
            }
        }
        identifisertKodeverk.isEmpty() &&
            Diagnosekoder.icd10.containsKey(diagnosekode) &&
            !Diagnosekoder.icpc2.containsKey(diagnosekode) -> {
            return CV().apply {
                s = Diagnosekoder.ICD10_CODE
                v = diagnosekode
                dn = Diagnosekoder.icd10[diagnosekode]?.text ?: ""
            }
        }
        identifisertKodeverk.isEmpty() &&
            Diagnosekoder.icpc2.containsKey(diagnosekode) &&
            !Diagnosekoder.icd10.containsKey(diagnosekode) -> {
            return CV().apply {
                s = Diagnosekoder.ICPC2_CODE
                v = diagnosekode
                dn = Diagnosekoder.icpc2[diagnosekode]?.text ?: ""
            }
        }
        else -> {
            throw IllegalStateException(
                "Diagnosekode $originalDiagnosekode tilhører ingen kjente kodeverk",
            )
        }
    }
}
