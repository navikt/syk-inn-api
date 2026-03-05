package modules.sykmelder.clients.pdl

import core.Environment
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.server.plugins.di.annotations.Named
import modules.sykmelder.clients.texas.TexasCloudClient

sealed interface PdlClient {
    suspend fun getPerson(ident: String): PdlPerson?
}

class PdlCloudClient(
    @Named("RetryHttpClient") private val httpClient: HttpClient,
    private val texasClient: TexasCloudClient,
    private val environment: Environment,
) : PdlClient {

    override suspend fun getPerson(ident: String): PdlPerson? {
        val (token) = getToken()

        val response =
            httpClient.get("${environment.external().tsmPdlCache}/api/person") {
                headers {
                    append("Nav-Consumer-Id", "syk-inn-api")
                    append("Authorization", "Bearer $token")
                    append("Ident", ident)
                }
            }

        return when {
            response.status.isSuccess() -> response.body<PdlPerson>()
            response.status == HttpStatusCode.NotFound -> null
            else -> {
                throw RuntimeException("Unable to get person from pdl for ident: $ident")
            }
        }
    }

    private suspend fun getToken() = texasClient.requestToken("tsm", "tsm-pdl-cache")
}
