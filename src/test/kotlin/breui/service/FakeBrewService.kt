package breui.service

import breui.model.Package
import breui.model.PackageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeBrewService : BrewService {
    var installedResult: Result<List<Package>> = Result.success(FIXTURE_PACKAGES)
    var searchResult: Result<List<Package>> = Result.success(FIXTURE_PACKAGES)
    var infoResult: Result<Package> = Result.success(FIXTURE_PACKAGES.first())
    var pinCalled = false
    var unpinCalled = false
    var uninstallCalled = false

    override suspend fun listInstalled() = installedResult
    override suspend fun search(query: String) = searchResult
    override suspend fun info(name: String, type: PackageType) = infoResult
    override fun install(name: String, type: PackageType): Flow<String> = flowOf("Installing $name...")
    override fun upgrade(name: String, type: PackageType): Flow<String> = flowOf("Upgrading $name...")
    override fun upgradeAll(): Flow<String> = flowOf("Upgrading all...")
    override fun uninstall(name: String, type: PackageType): Flow<String> {
        uninstallCalled = true
        return flowOf("Uninstalling $name...")
    }
    override suspend fun pin(name: String): Result<Unit> { pinCalled = true; return Result.success(Unit) }
    override suspend fun unpin(name: String): Result<Unit> { unpinCalled = true; return Result.success(Unit) }
    override suspend fun listTaps(): Result<List<String>> = Result.success(listOf("homebrew/core", "homebrew/cask"))
    override fun addTap(tap: String): Flow<String> = flowOf("Tapping $tap...")
    override suspend fun removeTap(tap: String): Result<Unit> = Result.success(Unit)

    companion object {
        val FIXTURE_PACKAGES = listOf(
            Package("curl", "8.5.0", PackageType.FORMULA, true, false, false,
                "Get a file from an HTTP, HTTPS or FTP server", "https://curl.se", "curl", listOf("openssl@3")),
            Package("git", "2.43.0", PackageType.FORMULA, true, false, true,
                "Distributed revision control system", "https://git-scm.com", "GPL-2.0-only", listOf("gettext", "pcre2")),
            Package("iterm2", "3.5.0", PackageType.CASK, true, false, false,
                "Terminal emulator as alternative to Apple Terminal", "https://iterm2.com", null, emptyList())
        )
    }
}
