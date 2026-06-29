package breui.service

import breui.model.Package
import breui.model.PackageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import java.io.IOException

class BrewServiceImpl : BrewService {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun listInstalled(): Result<List<Package>> = runCatching {
        val output = runCommand(listOf("brew", "info", "--json=v2", "--installed"))
        val response = json.decodeFromString<BrewListResponse>(output)
        val formulae = response.formulae.map { it.toPackage() }
        val casks = response.casks.map { it.toPackage() }
        (formulae + casks).sortedBy { it.name }
    }

    override suspend fun search(query: String): Result<List<Package>> = runCatching {
        val output = runCommand(listOf("brew", "search", "--json=v2", query))
        val response = json.decodeFromString<BrewSearchResponse>(output)
        val formulae = response.formulae.map { name ->
            Package(name, "", PackageType.FORMULA, false, false, false, "", "", null, emptyList())
        }
        val casks = response.casks.map { name ->
            Package(name, "", PackageType.CASK, false, false, false, "", "", null, emptyList())
        }
        (formulae + casks).sortedBy { it.name }
    }

    override suspend fun info(name: String, type: PackageType): Result<Package> = runCatching {
        val args = if (type == PackageType.CASK) {
            listOf("brew", "info", "--json=v2", "--cask", name)
        } else {
            listOf("brew", "info", "--json=v2", name)
        }
        val output = runCommand(args)
        val response = json.decodeFromString<BrewInfoResponse>(output)
        response.formulae.firstOrNull()?.toPackage()
            ?: response.casks.firstOrNull()?.toPackage()
            ?: error("No info found for $name")
    }

    override fun install(name: String, type: PackageType): Flow<String> = flow {
        emit("Not yet implemented")
    }

    override fun upgrade(name: String, type: PackageType): Flow<String> = flow {
        emit("Not yet implemented")
    }

    override fun upgradeAll(): Flow<String> = flow { emit("Not yet implemented") }

    override fun uninstall(name: String, type: PackageType): Flow<String> {
        val args = if (type == PackageType.CASK) {
            listOf("brew", "uninstall", "--cask", name)
        } else {
            listOf("brew", "uninstall", name)
        }
        return streamCommand(args)
    }

    override suspend fun pin(name: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Implemented in Task 12"))

    override suspend fun unpin(name: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Implemented in Task 12"))

    override suspend fun listTaps(): Result<List<String>> =
        Result.failure(UnsupportedOperationException("Implemented in Task 13"))

    override fun addTap(tap: String): Flow<String> = flow { emit("Not yet implemented") }

    override suspend fun removeTap(tap: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Implemented in Task 13"))

    private fun streamCommand(args: List<String>): Flow<String> = flow {
        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().use { reader ->
            reader.lineSequence().forEach { line -> emit(line) }
        }
        process.waitFor()
    }.flowOn(Dispatchers.IO)

    private suspend fun runCommand(args: List<String>): String = withContext(Dispatchers.IO) {
        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) throw IOException("Command failed (exit $exitCode): ${args.joinToString(" ")}\n$output")
        output
    }
}

private fun FormulaDto.toPackage() = Package(
    name = name,
    version = installed.firstOrNull()?.version ?: versions.stable,
    type = PackageType.FORMULA,
    installed = installed.isNotEmpty(),
    pinned = pinned,
    outdated = outdated,
    desc = desc,
    homepage = homepage,
    license = license,
    dependencies = dependencies
)

private fun CaskDto.toPackage() = Package(
    name = token,
    version = installed ?: version,
    type = PackageType.CASK,
    installed = installed != null,
    pinned = false,
    outdated = outdated,
    desc = desc,
    homepage = homepage,
    license = null,
    dependencies = emptyList()
)
