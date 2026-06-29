package breui.viewmodel

import breui.model.*
import breui.service.BrewService
import breui.service.TldrService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val brewService: BrewService,
    private val tldrService: TldrService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private fun update(block: AppState.() -> AppState) = _state.update(block)

    fun loadInstalled() {
        scope.launch {
            update { copy(loading = true) }
            brewService.listInstalled()
                .onSuccess { packages ->
                    update { copy(packages = packages, loading = false, selected = 0) }
                }
                .onFailure { e ->
                    update { copy(loading = false) }
                    setStatusMessage("Error: ${e.message}")
                }
        }
    }

    fun selectPackage(index: Int) {
        update { copy(selected = index) }
        if (_state.value.mode == Mode.SEARCH) {
            loadPackageInfo(index)
        }
    }

    fun search(query: String) {
        scope.launch {
            update { copy(loading = true, searchQuery = query) }
            brewService.search(query)
                .onSuccess { packages ->
                    update { copy(packages = packages, loading = false, selected = 0) }
                }
                .onFailure { e ->
                    update { copy(loading = false) }
                    setStatusMessage("Search error: ${e.message}")
                }
        }
    }

    fun loadPackageInfo(index: Int) {
        val pkg = _state.value.packages.getOrNull(index) ?: return
        scope.launch {
            brewService.info(pkg.name, pkg.type)
                .onSuccess { fullPkg ->
                    update {
                        copy(packages = packages.toMutableList().also { it[index] = fullPkg })
                    }
                }
                .onFailure { e -> setStatusMessage("Info error: ${e.message}") }
        }
    }

    fun setStatusMessage(message: String) {
        update { copy(statusMessage = message) }
        scope.launch {
            delay(3_000)
            update { copy(statusMessage = "") }
        }
    }

    fun toggleMode() {
        val newMode = if (_state.value.mode == Mode.INSTALLED) Mode.SEARCH else Mode.INSTALLED
        update { copy(mode = newMode, packages = emptyList(), selected = 0, searchQuery = "") }
        if (newMode == Mode.INSTALLED) loadInstalled()
    }

    fun setDetailTab(tab: DetailTab) {
        update { copy(detailTab = tab) }
    }
}
