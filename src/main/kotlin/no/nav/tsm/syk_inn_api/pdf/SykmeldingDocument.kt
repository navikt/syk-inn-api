package no.nav.tsm.syk_inn_api.pdf

import java.util.Base64
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.displayName
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import org.intellij.lang.annotations.Language

private object HtmlResources {
    val b64Logo =
        this.javaClass.getResourceAsStream("/pdf/logo.svg").use { stream ->
            stream.readBytes().let { bytes -> Base64.getEncoder().encodeToString(bytes) }
        }
}

fun FlowContent.NavHeader(title: String) {
    div(classes = "header") {
        img {
            src = "data:image/svg+xml;base64,${HtmlResources.b64Logo}"
            alt = "NAV Logo"
        }
        h1 { +title }
    }
}

fun TR.TableInfo(title: String, colspan: String? = null, value: () -> String) {
    td {
        if (colspan != null) {
            this.colSpan = colspan
        }
        div(classes = "title") { +title }
        div(classes = "value") { +value() }
    }
}

fun buildSykmeldingHtml(sykmelding: SykmeldingDocument, pasient: Person): String {
    val htmlContent =
        createHTML(prettyPrint = true, xhtmlCompatible = true).html {
            head {
                title("Sykmelding")
                meta(name = "subject", "Sykmelding")
                meta(name = "author", "syk-inn-api")
                meta(name = "description", "TODO")
                meta(charset = "UTF-8")

                style(type = "text/css") { unsafe { +stylesheet } }
            }

            body {
                NavHeader(title = "Innsendt sykmelding")

                div(classes = "content") {
                    table(classes = "info-table") {
                        tr {
                            TableInfo("Navn") { "${pasient.displayName()} (${pasient.ident})" }
                            TableInfo("Mottatt av Nav") { "TODO: Mangler i sykmelding" }
                        }
                        tr {
                            TableInfo("Arbeidsgiver") {
                                if (sykmelding.values.arbeidsgiver != null) {
                                    sykmelding.values.arbeidsgiver.arbeidsgivernavn
                                } else {
                                    "Ingen arbeidsgiver oppgitt"
                                }
                            }
                            TableInfo("Periode") { "TODO: Formatter periode" }
                        }
                        tr {
                            TableInfo("Mulighet for arbeid") {
                                "100% sykmeldt (TODO: dette er hardkoda)"
                            }
                            TableInfo("Sykmeldingsgrad (%)") {
                                "100% sykmeldt (TODO: dette er hardkoda"
                            }
                        }
                        tr {
                            TableInfo("Diagnose") {
                                if (sykmelding.values.hoveddiagnose != null) {
                                    "${sykmelding.values.hoveddiagnose.code}: ${sykmelding.values.hoveddiagnose.text} (${sykmelding.values.hoveddiagnose.system.code})"
                                } else {
                                    "Ingen diagnose oppgitt"
                                }
                            }
                            TableInfo("Andre spørsmål") { "TODO: Er det svangerskapsrelatert" }
                        }
                        tr {
                            TableInfo("Melding til Nav", colspan = "2") {
                                sykmelding.values.meldinger.tilNav ?: ""
                            }
                        }
                        tr {
                            TableInfo("Innspill til arbeidsgiver", colspan = "2") {
                                sykmelding.values.meldinger.tilArbeidsgiver ?: ""
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
@page {
    font-family: 'Source Sans Pro', sans-serif;
    margin: 0;
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
    padding: 2cm;
    padding-top: 1cm;
}

.header {
    background-color: #E6F0FF;
    padding: 0.75cm;
    padding-left: 2cm;
    height: 0.5cm;
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

.info-table {
    width: 100%;
}

.info-table td {
    width: 50%;
}

.info-table .title {
    font-weight: bold;
}

.info-table .value {
    margin-bottom: 0.75cm;
}
"""
        .trimIndent()
