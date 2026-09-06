// This is the Kotlin code used to generate a client in HelseID selvbetjening.
// https://api.selvbetjening.nhn.no/docs/v1/index.html#tag/ClientDrafts

data class ClientDraft(
  val organizationNumber: String,
  val publicJwk: String,
  val postClientConfirmationRedirectUri: String,
)

val helseIdHttp =
  HttpClient(CIO) {
    install(Logging)
    install(ContentNegotiation) { jackson { enable(SerializationFeature.INDENT_OUTPUT) } }
  }

routing {
  route("/create-client") { // navigate to http://localhost:8080/create-client in browser
    get {
      val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()
      val jwk =
        RSAKey.Builder(kp.public as RSAPublicKey)
          .privateKey(kp.private as RSAPrivateKey)
          .keyUse(KeyUse.SIGNATURE)
          .keyID(UUID.randomUUID().toString())
          .algorithm(JWSAlgorithm.RS256)
          .build()

      log.info("\n================================\n")
      log.info("SECRET - full JWK for nais: ${jwk.toJSONString()}") // private, goes in the secret
      log.info("\n================================\n")

      val mapper = jacksonObjectMapper()
      val httpResponse =
        helseIdHttp.post("https://api.selvbetjening.nhn.no/v1/client-drafts") {
          contentType(ContentType.Application.Json)
          header("api-key", "YOUR API KEY") // TODO
          setBody(
            ClientDraft(
              "889640782",
              jwk.toPublicJWK().toJSONString(),
              "http://localhost:8080/client-confirm",
            )
          )
        }

      val body = httpResponse.bodyAsText()
      when {
        httpResponse.status.isSuccess() -> {
          val clientId = mapper.readTree(body)["clientId"].asString()
          log.info("clientId: $clientId")
          log.info("confirm here: https://selvbetjening.nhn.no/confirm-client/$clientId")
          call.respondText("Confirm at: https://selvbetjening.nhn.no/confirm-client/$clientId")
        }

        httpResponse.status == HttpStatusCode.BadRequest -> {
          val err = mapper.readTree(body)
          log.error(
            "draft rejected: property=${err["property"]?.asString()} message=${err["message"]?.asString()}"
          )
          call.respondText("Bad request: $body", status = HttpStatusCode.BadRequest)
        }

        else -> {
          log.error("unexpected ${httpResponse.status}: $body")
          call.respondText(
            "Failed (${httpResponse.status}): $body",
            status = HttpStatusCode.InternalServerError,
          )
        }
      }
    }
  }
}