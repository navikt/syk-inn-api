package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.syk_inn_api.client.IHelsenettProxyClient

import no.nav.tsm.syk_inn_api.model.Sykmelder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HelsenettProxyService(
    private val helsenettProxyClient: IHelsenettProxyClient,
) {
    private val logger = LoggerFactory.getLogger(HelsenettProxyService::class.java)

    fun getSykmelderByHpr(hpr: String, sykmeldingId: String): Sykmelder {
        logger.info(
            "Getting sykmelder for hpr=$hpr, sykmeldingId=$sykmeldingId",
        )
        val result = helsenettProxyClient.getSykmelderByHpr(hpr, sykmeldingId)

        return result.fold({ it }) {
            logger.error(
                "Error while fetching sykmelder for hpr=$hpr, sykmeldingId=$sykmeldingId", it,
            )
            throw it
        }
    }
}
