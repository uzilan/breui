package breui.service

import breui.model.Package
import breui.model.PackageType
import kotlinx.coroutines.flow.Flow

interface BrewService {
    suspend fun listInstalled(): Result<List<Package>>
    suspend fun search(query: String): Result<List<Package>>
    suspend fun info(name: String, type: PackageType): Result<Package>
    fun install(name: String, type: PackageType): Flow<String>
    fun upgrade(name: String, type: PackageType): Flow<String>
    fun upgradeAll(): Flow<String>
    fun uninstall(name: String, type: PackageType): Flow<String>
    suspend fun pin(name: String): Result<Unit>
    suspend fun unpin(name: String): Result<Unit>
    suspend fun listTaps(): Result<List<String>>
    fun addTap(tap: String): Flow<String>
    suspend fun removeTap(tap: String): Result<Unit>
}
