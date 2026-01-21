package no.nav.tsm.syk_inn_api.pdf

import java.util.Base64
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import no.nav.tsm.syk_inn_api.pdf.SykmeldingHTMLUtils.svangerskapsrelatertText
import no.nav.tsm.syk_inn_api.pdf.SykmeldingHTMLUtils.yrkesskadeText
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.displayName
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.utils.toReadableDate
import no.nav.tsm.syk_inn_api.utils.toReadableDatePeriod
import org.intellij.lang.annotations.Language

private object HtmlResources {
    val b64Logo: String? =
        this.javaClass.getResourceAsStream("/pdf/logo.svg").use { stream ->
            stream?.readBytes()?.let { bytes -> Base64.getEncoder().encodeToString(bytes) }
        }
}

fun FlowContent.navHeader(title: String) {
    div(classes = "header") {
        img {
            src = "data:image/svg+xml;base64,${HtmlResources.b64Logo}"
            alt = "NAV Logo"
        }
        h1 { +title }
    }
}

fun TR.tableInfo(
    title: String,
    colspan: String? = null,
    italic: Boolean = false,
    value: () -> String
) {
    td {
        if (colspan != null) {
            this.colSpan = colspan
        }
        div(classes = "title") { +title }
        div(
            classes = "value".let { if (italic) "$it italic" else it },
        ) {
            +value()
        }
    }
}

fun TR.tableInfoMultiRow(
    title: String,
    colspan: String? = null,
    italic: Boolean = false,
    value: () -> List<String>
) {
    td {
        if (colspan != null) {
            this.colSpan = colspan
        }
        div(classes = "title") { +title }
        div(
            classes = "value".let { if (italic) "$it italic" else it },
        ) {
            value().map { line -> div { +line } }
        }
    }
}

fun buildSykmeldingHtml(sykmelding: SykmeldingDocument, pasient: Person): String {
    val andreSporsmalTexts: List<String>? =
        listOfNotNull(
                svangerskapsrelatertText(sykmelding.values.svangerskapsrelatert),
                yrkesskadeText(sykmelding.values.yrkesskade),
            )
            .ifEmpty { null }
    val utdypendeSporsmal = sykmelding.values.utdypendeSporsmal
    val htmlContent =
        createHTML(prettyPrint = true, xhtmlCompatible = true).html {
            head {
                title("Sykmelding")
                meta(name = "subject", "Sykmelding")
                meta(name = "author", "syk-inn-api")
                meta(
                    name = "description",
                    "Sykmelding i perioden ${
                        toReadableDatePeriod(
                            sykmelding.values.aktivitet.first().fom,
                            sykmelding.values.aktivitet.last().tom,
                        )
                    }",
                )
                meta(charset = "UTF-8")

                style(type = "text/css") { unsafe { +stylesheet } }
            }

            body {
                /** This is fixed at the top across every page */
                navHeader(title = "Innsendt sykmelding")

                /*
                   This is fixed in the bottom left corner of every page
                */
                div(classes = "footer") { +sykmelding.sykmeldingId }

                div(classes = "content") {
                    table(classes = "info-table") {
                        tbody(classes = "keep-together") {
                            tr {
                                tableInfo("Navn") { "${pasient.displayName()} (${pasient.ident})" }
                                tableInfo("Mottatt av Nav") {
                                    toReadableDate(sykmelding.meta.mottatt.toLocalDate())
                                }
                            }
                            tr {
                                tableInfoMultiRow("Sykmelder") {
                                    listOf(
                                        "${sykmelding.meta.sykmelder.fornavn} ${sykmelding.meta.sykmelder.mellomnavn ?: ""} ${sykmelding.meta.sykmelder.etternavn}",
                                        "HPR-nr: ${sykmelding.meta.sykmelder.hprNummer}",
                                    )
                                }
                                tableInfoMultiRow("Legekontor", colspan = "2") {
                                    listOf(
                                        "Org.nr.: ${sykmelding.meta.legekontorOrgnr ?: "Ikke oppgitt"}",
                                        "Tlf: ${sykmelding.meta.legekontorTlf ?: "Ikke oppgitt"}",
                                    )
                                }
                            }
                        }

                        sykmelding.values.arbeidsgiver?.let {
                            if (it.harFlere) {
                                tbody(classes = "keep-together") {
                                    tr {
                                        tableInfo(
                                            "Arbeidsgiver",
                                            italic = sykmelding.values.arbeidsgiver == null,
                                        ) {
                                            sykmelding.values.arbeidsgiver.arbeidsgivernavn
                                        }
                                    }
                                }
                            }
                        }

                        tbody(classes = "keep-together") {
                            tr {
                                tableInfoMultiRow("Sykmeldingsperiode", colspan = "2") {
                                    sykmelding.values.aktivitet.flatMap {
                                        SykmeldingHTMLUtils.formatReadablePeriode(it)
                                    }
                                }
                            }
                        }
                        tbody(classes = "keep-together") {
                            tr {
                                tableInfo("Diagnose") {
                                    if (sykmelding.values.hoveddiagnose != null) {
                                        "${sykmelding.values.hoveddiagnose.code}: ${sykmelding.values.hoveddiagnose.text} (${sykmelding.values.hoveddiagnose.system.name})"
                                    } else {
                                        "Ingen diagnose oppgitt"
                                    }
                                }
                                tableInfoMultiRow("Bidiagnoser") {
                                    if (sykmelding.values.bidiagnoser?.isNotEmpty() == true) {
                                        sykmelding.values.bidiagnoser.map {
                                            "${it.code}: ${it.text} (${it.system.name})"
                                        }
                                    } else {
                                        listOf("Ingen bidiagnoser oppgitt")
                                    }
                                }
                            }
                            if (sykmelding.values.annenFravarsgrunn != null) {
                                tr {
                                    tableInfo("Annen lovfestet fraværsgrunn", colspan = "2") {
                                        SykmeldingHTMLUtils.annenFravarsgrunnToText(
                                            sykmelding.values.annenFravarsgrunn
                                        )
                                    }
                                }
                            }
                        }
                        if (utdypendeSporsmal != null) {
                            if (utdypendeSporsmal.utfordringerMedArbeid != null) {
                                tbody(classes = "keep-together") {
                                    tr {
                                        tableInfo(
                                            "Hvilke utfordringer har pasienten med å utføre gradert arbeid?",
                                            colspan = "2",
                                        ) {
                                            utdypendeSporsmal.utfordringerMedArbeid
                                        }
                                    }
                                }
                            }
                            if (utdypendeSporsmal.medisinskOppsummering != null) {
                                tbody(classes = "keep-together") {
                                    tr {
                                        tableInfo(
                                            "Gi en kort medisinsk oppsummering av tilstanden (sykehistorie, hovedsymptomer, pågående/planlagt behandling)",
                                            colspan = "2",
                                        ) {
                                            utdypendeSporsmal.medisinskOppsummering
                                        }
                                    }
                                }
                            }
                            if (utdypendeSporsmal.hensynPaArbeidsplassen != null) {
                                tbody(classes = "keep-together") {
                                    tr {
                                        tableInfo(
                                            "Hvilke hensyn må være på plass for at pasienten kan prøves i det nåværende arbeidet? (ikke obligatorisk)",
                                            colspan = "2",
                                        ) {
                                            utdypendeSporsmal.hensynPaArbeidsplassen
                                        }
                                    }
                                }
                            }
                        }
                        if (andreSporsmalTexts != null) {
                            tbody(classes = "keep-together") {
                                tr {
                                    tableInfoMultiRow("Andre spørsmål", colspan = "2") {
                                        andreSporsmalTexts
                                    }
                                }
                            }
                        }
                        tbody(classes = "keep-together") {
                            tr {
                                tableInfo(
                                    "Melding til Nav",
                                    colspan = "2",
                                    italic = sykmelding.values.meldinger.tilNav == null,
                                ) {
                                    sykmelding.values.meldinger.tilNav ?: "Ingen melding til Nav"
                                }
                            }
                        }
                        tbody(classes = "keep-together") {
                            tr {
                                tableInfo(
                                    "Innspill til arbeidsgiver",
                                    colspan = "2",
                                    italic = sykmelding.values.meldinger.tilArbeidsgiver == null,
                                ) {
                                    sykmelding.values.meldinger.tilArbeidsgiver
                                        ?: "Ingen melding til arbeidsgiver"
                                }
                            }
                        }
                        if (sykmelding.values.pasientenSkalSkjermes) {
                            tbody(classes = "keep-together") {
                                tr {
                                    tableInfo(
                                        "Pasienten er skjermet for medisinske opplysninger",
                                        colspan = "2",
                                        italic = false,
                                    ) {
                                        "Ja"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    return htmlContent
}

@Language("CSS")
val stylesheet =
    """
* {
    font-family: 'Source Sans Pro', sans-serif;
}

@page {
    margin: 20mm 0 20mm 0;

    @top-center {
       width: 100%;
       content: element(header);
    }

    @bottom-left {
      font-family: 'Source Sans Pro', sans-serif;
	  content: element(footer);
      padding-left: 10mm;
	}

    @bottom-right {
      font-family: 'Source Sans Pro', sans-serif;
	  content: 'Side ' counter(page) ' av ' counter(pages);
	  padding-right: 10mm;
	}
}

html {
    padding: 0;
    margin: 0;
}

body {
    font-family: 'Source Sans Pro', sans-serif;
    padding: 0;
    margin: 0;
}

.content {
    padding: 0 20mm;
}

.header {
    position: running(header);
    background-color: #E6F0FF;
    padding: 0.65cm;
    padding-left: 2cm;
}

.header img {
    width: 64px;
    height: auto;
    float: left;
    margin-right: 32px;
}

.header h1 {
    font-size: 22px;
    padding: 0;
    margin: 0;
    margin-top: -4px;
    padding-left: 32px;
}

.footer {
  position: running(footer);
  color: #797979;
}

.info-table {
    width: 100%;
}

.info-table td {
    vertical-align: top;
    width: 50%;
}

.info-table .title {
    margin-top: 0.75cm;
    font-weight: bold;
}

.info-table .value {
}

.info-table .value.italic {
    color: #333;
    font-style: italic;
}

.keep-together {
  page-break-inside: avoid;
  break-inside: avoid;        /* modern */
  -fs-page-break-inside: avoid; /* openhtmltopdf / Flying Saucer */
}
"""
        .trimIndent()
