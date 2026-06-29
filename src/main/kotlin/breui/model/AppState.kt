package breui.model

enum class Mode { INSTALLED, SEARCH }
enum class DetailTab { INFO, DEPS, TLDR }

data class AppState(
    val mode: Mode = Mode.INSTALLED,
    val packages: List<Package> = emptyList(),
    val selected: Int = 0,
    val searchQuery: String = "",
    val detailTab: DetailTab = DetailTab.INFO,
    val loading: Boolean = false,
    val statusMessage: String = "",
    val overlay: Overlay? = null
)
