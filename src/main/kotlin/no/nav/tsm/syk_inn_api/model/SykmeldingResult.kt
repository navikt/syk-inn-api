package no.nav.tsm.syk_inn_api.model

import no.nav.tsm.syk_inn_api.sykmeldingresponse.SykmeldingResponse
import org.springframework.http.HttpStatus

sealed interface SykmeldingResult {
    data class Success(
        val sykmeldingResponse: SykmeldingResponse? = null,
        val sykmeldinger: List<SykmeldingResponse> = emptyList(),
        val statusCode: HttpStatus,
        val pdf: ByteArray? = null
    ) : SykmeldingResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Success

            if (pdf != null) {
                if (other.pdf == null) return false
                if (!pdf.contentEquals(other.pdf)) return false
            } else if (other.pdf != null) return false

            return true
        }

        override fun hashCode(): Int {
            return pdf?.contentHashCode() ?: 0
        }
    }

    data class Failure(
        val errorMessage: String,
        val errorCode: HttpStatus,
    ) : SykmeldingResult
}
