package breui.service

import breui.model.Package
import breui.model.PackageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class StubBrewService : BrewService {
    override suspend fun listInstalled() = Result.success(listOf(
        Package("curl", "8.5.0", PackageType.FORMULA, true, false, false, "HTTP client", "https://curl.se", "curl", listOf("openssl@3")),
        Package("git", "2.43.0", PackageType.FORMULA, true, false, true, "Version control", "https://git-scm.com", "GPL-2.0-only", listOf("gettext")),
    ))
    override suspend fun search(query: String) = Result.success(emptyList<Package>())
    override suspend fun info(name: String, type: PackageType) = Result.failure<Package>(UnsupportedOperationException())
    override fun install(name: String, type: PackageType): Flow<String> = flowOf()
    override fun upgrade(name: String, type: PackageType): Flow<String> = flowOf()
    override fun upgradeAll(): Flow<String> = flowOf()
    override fun uninstall(name: String, type: PackageType): Flow<String> = flowOf()
    override suspend fun pin(name: String) = Result.success(Unit)
    override suspend fun unpin(name: String) = Result.success(Unit)
    override suspend fun listTaps() = Result.success(emptyList<String>())
    override fun addTap(tap: String): Flow<String> = flowOf()
    override suspend fun removeTap(tap: String) = Result.success(Unit)
}
