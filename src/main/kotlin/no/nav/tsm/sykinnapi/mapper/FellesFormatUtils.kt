package no.nav.tsm.sykinnapi.mapper

import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet

fun extractHelseOpplysningerArbeidsuforhet(
    fellesformat: XMLEIFellesformat
): HelseOpplysningerArbeidsuforhet =
    fellesformat.get<XMLMsgHead>().document[0].refDoc.content.any[0]
        as HelseOpplysningerArbeidsuforhet

inline fun <reified T> XMLEIFellesformat.get() = this.any.find { it is T } as T
