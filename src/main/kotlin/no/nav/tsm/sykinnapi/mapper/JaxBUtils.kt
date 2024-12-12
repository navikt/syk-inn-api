package no.nav.tsm.sykinnapi.mapper

import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Marshaller.JAXB_ENCODING
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet

val fellesformatMarshaller: Marshaller =
    JAXBContext.newInstance(
            XMLEIFellesformat::class.java,
            XMLMsgHead::class.java,
            HelseOpplysningerArbeidsuforhet::class.java,
        )
        .createMarshaller()
        .apply { setProperty(JAXB_ENCODING, "UTF-8") }

fun Marshaller.toString(input: Any): String =
    StringWriter().use {
        marshal(input, it)
        it.toString()
    }
