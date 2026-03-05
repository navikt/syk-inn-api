package modules.sykmelder.clients.btsys

class BtsysLocalClient : BtsysClient {
    override suspend fun isSuspendert(hpr: String): Boolean {
        return false
    }
}
