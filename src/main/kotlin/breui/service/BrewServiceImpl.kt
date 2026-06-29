package breui.service

import breui.model.Package
import breui.model.PackageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
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

    override suspend fun search(query: String): Result<List<Package>> =
        Result.failure(UnsupportedOperationException("Implemented in Task 7"))

    override suspend fun info(name: String, type: PackageType): Result<Package> =
        Result.failure(UnsupportedOperationException("Implemented in Task 7"))

    override fun install(name: String, type: PackageType): Flow<String> = flow {
        emit("Not yet implemented")
    }

    override fun upgrade(name: String, type: PackageType): Flow<String> = flow {
        emit("Not yet implemented")
    }

    override fun upgradeAll(): Flow<String> = flow { emit("Not yet implemented") }

    override fun uninstall(name: String, type: PackageType): Flow<String> = flow {
        emit("Not yet implemented")
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

    private suspend fun runCommand(args: List<String>): String = withContext(Dispatchers.IO) {
        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) throw IOException("Command failed (exit $exitCode): ${args.joinToString(" ")}\n$output")
        output
    }

    @Suppress("unused")
    private fun streamCommand(args: List<String>): Flow<String> = flow {
        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { emit(it) }
        }
        process.waitFor()
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
