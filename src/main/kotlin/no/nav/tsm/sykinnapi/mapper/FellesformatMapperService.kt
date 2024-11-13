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
                            s = "2.16.578.1.12.4.1.1.8116"
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
                dn = "Én arbeidsgiver"
                v = "1"
            }

        navnArbeidsgiver = ""
        yrkesbetegnelse = ""
        stillingsprosent = null
    }

fun tilMedisinskVurdering(
    hoveddiagnose: Hoveddiagnose,
): HelseOpplysningerArbeidsuforhet.MedisinskVurdering {

    return HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
        hovedDiagnose =
            HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
                diagnosekode = toMedisinskVurderingDiagnose(hoveddiagnose)
            }

        isSkjermesForPasient = false
        annenFraversArsak = null
        isSvangerskap = false
        isYrkesskade = false
        yrkesskadeDato = null
    }
}

fun toMedisinskVurderingDiagnose(hoveddiagnose: Hoveddiagnose): CV =
    CV().apply {
        s = toDiagnoseKithSystem(hoveddiagnose.system.toString())
        v = hoveddiagnose.code
        dn = getTextFromDiagnose(hoveddiagnose.code, hoveddiagnose.system.toString())
    }

fun getTextFromDiagnose(
    kode: String,
    diagnoseSystem: String,
): String {
    return when (diagnoseSystem) {
        "ICD10" -> {
            Diagnosekoder.icd10[kode]!!.text
        }
        "ICPC2" -> {
            Diagnosekoder.icpc2[kode]!!.text
        }
        else -> {
            throw MappingException("Ukjent diagnose kode")
        }
    }
}

fun toDiagnoseKithSystem(diagnoseSystem: String): String {
    return when (diagnoseSystem) {
        "ICD10" -> {
            "2.16.578.1.12.4.1.1.7110"
        }
        "ICPC2" -> {
            "2.16.578.1.12.4.1.1.7170"
        }
        else -> {
            throw MappingException("Ukjent diagnose system")
        }
    }
}

class MappingException(override val message: String) : Exception(message)
