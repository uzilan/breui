package breui.service

interface TldrService {
    suspend fun get(name: String): String?
}

class NoOpTldrService : TldrService {
    override suspend fun get(name: String): String? = null
}
