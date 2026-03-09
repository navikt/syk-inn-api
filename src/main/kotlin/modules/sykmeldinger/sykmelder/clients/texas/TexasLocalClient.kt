package modules.sykmeldinger.sykmelder.clients.texas

class TexasLocalClient : TexasClient {
    override suspend fun requestToken(namespace: String, otherApiAppName: String): TexasToken {
        return TexasToken("fake-token-for-${namespace}-${otherApiAppName}")
    }
}
