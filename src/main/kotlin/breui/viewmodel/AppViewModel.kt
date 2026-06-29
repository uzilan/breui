package breui.viewmodel

import breui.model.AppState
import breui.model.DetailTab
import breui.model.Mode
import breui.model.Overlay
import breui.model.Package
import breui.model.PackageType
import breui.service.BrewService
import breui.service.TldrService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
        if (tab == DetailTab.TLDR) loadTldr(_state.value.selected)
    }

    fun loadTldr(index: Int) {
        val pkg = _state.value.packages.getOrNull(index) ?: return
        if (pkg.tldr != null) return
        scope.launch {
            val result = tldrService.get(pkg.name)
            val tldrText = result ?: pkg.desc.ifBlank { "No description available" }
            update {
                val updated = packages.toMutableList()
                updated.getOrNull(index)?.let { updated[index] = it.copy(tldr = tldrText) }
                copy(packages = updated)
            }
        }
    }

    fun showConfirm(message: String, onConfirm: () -> Unit) {
        update { copy(overlay = Overlay.Confirm(message, onConfirm)) }
    }

    fun closeOverlay() {
        update { copy(overlay = null) }
    }

    fun openProgress(title: String) {
        update { copy(overlay = Overlay.Progress(title, emptyList())) }
    }

    fun appendProgressLine(line: String) {
        val current = _state.value.overlay as? Overlay.Progress ?: return
        update { copy(overlay = current.copy(lines = current.lines + line)) }
    }

    private suspend fun runWithProgress(title: String, flow: Flow<String>, onDone: suspend () -> Unit = {}) {
        openProgress(title)
        flow.collect { line -> appendProgressLine(line) }
        onDone()
        delay(500)
        closeOverlay()
    }

    fun installPackage(index: Int) {
        val pkg = _state.value.packages.getOrNull(index) ?: return
        scope.launch {
            runWithProgress("Installing ${pkg.name}", brewService.install(pkg.name, pkg.type)) {
                loadInstalled()
                setStatusMessage("Installed ${pkg.name}")
            }
        }
    }

    fun upgradePackage(index: Int) {
        val pkg = _state.value.packages.getOrNull(index) ?: return
        scope.launch {
            runWithProgress("Upgrading ${pkg.name}", brewService.upgrade(pkg.name, pkg.type)) {
                loadInstalled()
                setStatusMessage("Upgraded ${pkg.name}")
            }
        }
    }

    fun upgradeAll() {
        showConfirm("Upgrade all outdated packages?") {
            scope.launch {
                closeOverlay()
                runWithProgress("Upgrading all", brewService.upgradeAll()) {
                    loadInstalled()
                    setStatusMessage("Upgrade complete")
                }
            }
        }
    }

    fun togglePin(index: Int) {
        val pkg = _state.value.packages.getOrNull(index) ?: return
        if (pkg.type == PackageType.CASK) {
            setStatusMessage("Casks cannot be pinned")
            return
        }
        scope.launch {
            if (pkg.pinned) {
                brewService.unpin(pkg.name)
                    .onSuccess { loadInstalled(); setStatusMessage("Unpinned ${pkg.name}") }
                    .onFailure { e -> setStatusMessage("Error: ${e.message}") }
            } else {
                brewService.pin(pkg.name)
                    .onSuccess { loadInstalled(); setStatusMessage("Pinned ${pkg.name}") }
                    .onFailure { e -> setStatusMessage("Error: ${e.message}") }
            }
        }
    }

    fun uninstallPackage(index: Int) {
        val pkg = _state.value.packages.getOrNull(index) ?: return
        showConfirm("Uninstall ${pkg.name}?") {
            scope.launch {
                closeOverlay()
                setStatusMessage("Uninstalling ${pkg.name}...")
                brewService.uninstall(pkg.name, pkg.type).collect {}
                loadInstalled()
                setStatusMessage("Uninstalled ${pkg.name}")
            }
        }
    }
}
