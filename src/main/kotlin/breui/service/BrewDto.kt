package breui.service

import kotlinx.serialization.Serializable

@Serializable
data class BrewListResponse(
    val formulae: List<FormulaDto> = emptyList(),
    val casks: List<CaskDto> = emptyList()
)

@Serializable
data class FormulaDto(
    val name: String,
    val versions: VersionsDto = VersionsDto(""),
    val installed: List<InstalledVersionDto> = emptyList(),
    val pinned: Boolean = false,
    val outdated: Boolean = false,
    val desc: String? = null,
    val homepage: String? = null,
    val license: String? = null,
    val dependencies: List<String> = emptyList()
)

@Serializable
data class VersionsDto(val stable: String)

@Serializable
data class InstalledVersionDto(val version: String)

@Serializable
data class CaskDto(
    val token: String,
    val version: String = "",
    val installed: String? = null,
    val outdated: Boolean = false,
    val desc: String? = null,
    val homepage: String? = null
)

@Serializable
data class BrewInfoResponse(
    val formulae: List<FormulaDto> = emptyList(),
    val casks: List<CaskDto> = emptyList()
)

@Serializable
data class BrewSearchResponse(
    val formulae: List<String> = emptyList(),
    val casks: List<String> = emptyList()
)
