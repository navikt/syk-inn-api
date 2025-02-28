package no.nav.tsm.sykinnapi.client.smpdfgen

import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import no.nav.tsm.sykinnapi.modell.smpdfgen.Pasient
import no.nav.tsm.sykinnapi.modell.smpdfgen.PdfPayload
import no.nav.tsm.sykinnapi.modell.tsmpdl.PdlPerson
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class SmpdfgenClient(
    @Qualifier("smpdfgenClientRestClient") private val smpdfgenClientRestClient: RestClient,
) {

    private val logger = LoggerFactory.getLogger(SmpdfgenClient::class.java)

    fun createPdf(payload: PdfPayload) =
        smpdfgenClientRestClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/api/v1/genpdf/sm/sm").build() }
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .body(payload)
            .retrieve()
            .onStatus({ it.isError }) { req, res -> onStatusError(res) }
            .body<ByteArray>()
            ?: throw RuntimeException("Body is not ByteArray")

    private fun onStatusError(res: ClientHttpResponse): Nothing {
        throw RuntimeException("Error from smpdfgen got statuscode: ${res.statusCode}").also {
            logger.error(it.message, it)
        }
    }
}

fun createPdfPayload(
    receivedSykmelding: ReceivedSykmelding,
    pdlPerson: PdlPerson,
): PdfPayload =
    PdfPayload(
        pasient =
            Pasient(
                fornavn = pdlPerson.navn?.fornavn ?: "",
                mellomnavn = pdlPerson.navn?.mellomnavn ?: "",
                etternavn = pdlPerson.navn?.etternavn ?: "",
                personnummer = receivedSykmelding.personNrPasient,
                tlfNummer = receivedSykmelding.tlfPasient,
            ),
        sykmelding = receivedSykmelding.sykmelding,
        validationResult = ValidationResult.OK,
        mottattDato = receivedSykmelding.mottattDato,
        behandlerKontorOrgName = receivedSykmelding.legekontorOrgName,
        merknader = receivedSykmelding.merknader,
        rulesetVersion = receivedSykmelding.rulesetVersion,
        signerendBehandlerHprNr = receivedSykmelding.legeHprNr,
    )
