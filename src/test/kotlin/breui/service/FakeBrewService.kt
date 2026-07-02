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
    override suspend fun update(): Result<Unit> = Result.success(Unit)

    companion object {
        val FIXTURE_PACKAGES = listOf(
            Package(
                name = "curl",
                version = "8.5.0",
                type = PackageType.FORMULA,
                installed = true,
                pinned = false,
                outdated = false,
                desc = "Get a file from an HTTP, HTTPS or FTP server",
                homepage = "https://curl.se",
                license = "curl",
                dependencies = listOf("openssl@3")
            ),
            Package(
                name = "git",
                version = "2.43.0",
                type = PackageType.FORMULA,
                installed = true,
                pinned = false,
                outdated = true,
                desc = "Distributed revision control system",
                homepage = "https://git-scm.com",
                license = "GPL-2.0-only",
                dependencies = listOf("gettext", "pcre2")
            ),
            Package(
                name = "iterm2",
                version = "3.5.0",
                type = PackageType.CASK,
                installed = true,
                pinned = false,
                outdated = false,
                desc = "Terminal emulator as alternative to Apple Terminal",
                homepage = "https://iterm2.com",
                license = null,
                dependencies = emptyList()
            )
        )
    }
}
