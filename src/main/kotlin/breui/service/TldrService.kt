package breui.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface TldrService {
    suspend fun get(name: String): String?
}

class NoOpTldrService : TldrService {
    override suspend fun get(name: String): String? = null
}

class TldrServiceImpl : TldrService {
    private val cache = mutableMapOf<String, String?>()

    override suspend fun get(name: String): String? {
        if (cache.containsKey(name)) return cache[name]
        val result = runCatching { fetchTldr(name) }.getOrNull()
        cache[name] = result
        return result
    }

    private suspend fun fetchTldr(name: String): String? = withContext(Dispatchers.IO) {
        val process = ProcessBuilder("tldr", name)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()
        if (exitCode != 0 || output.isBlank() || output.contains("This page doesn't exist")) null
        else output
    }
}
