package breui.model

enum class PackageType { FORMULA, CASK }

data class Package(
    val name: String,
    val version: String,
    val type: PackageType,
    val installed: Boolean,
    val pinned: Boolean,
    val outdated: Boolean,
    val desc: String,
    val homepage: String,
    val license: String?,
    val dependencies: List<String>,
    val tldr: String? = null
)
