# breui — Design Document

**Date:** 2026-06-29  
**Status:** Approved

---

## Overview

Terminal-based Homebrew management UI for macOS. Written in Kotlin, built as a fat JAR. Comparable in feel to `btop` or `lazygit`. Manages both formulae and casks in a unified view.

---

## Key Decisions

| Decision | Choice | Reason |
|---|---|---|
| Formulae vs casks | Both, unified | Casks treated as packages with empty deps list |
| Distribution | Fat JAR (`java -jar breui.jar`) | Simple; macOS Homebrew users likely have JVM |
| Search trigger | Enter key | `brew search` is slow (~1-2s); on-Enter avoids spam |
| Build order | Vertical slices | Catches Lanterna quirks early; each slice is runnable |

---

## Technology Stack

| Layer | Choice |
|---|---|
| Language | Kotlin (JVM), Gradle `.kts` |
| TUI framework | Lanterna |
| Async | Kotlin Coroutines + StateFlow |
| Fat JAR | Gradle Shadow plugin |

---

## Project Structure

```
breui/
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   ├── main/kotlin/breui/
│   │   ├── Main.kt
│   │   ├── model/            # Package, AppState, Overlay, enums
│   │   ├── service/          # BrewService (interface + impl), TldrService
│   │   ├── viewmodel/        # AppViewModel
│   │   └── ui/
│   │       ├── App.kt        # Lanterna setup, event loop
│   │       ├── ListPanel.kt
│   │       ├── DetailPanel.kt
│   │       ├── StatusBar.kt
│   │       └── overlays/     # Confirm, TapManager, Progress
│   └── test/kotlin/breui/
│       ├── service/          # BrewServiceTest
│       └── viewmodel/        # AppViewModelTest
```

---

## Data Model

```kotlin
enum class PackageType { FORMULA, CASK }
enum class Mode { INSTALLED, SEARCH }
enum class DetailTab { INFO, DEPS, TLDR }

data class Package(
    val name: String,
    val version: String,
    val type: PackageType,
    val installed: Boolean,
    val pinned: Boolean,
    val outdated: Boolean,
    val desc: String,
    val homepage: String,
    val license: String?,           // null for casks
    val dependencies: List<String>, // empty for casks
    val tldr: String? = null
)

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

sealed class Overlay {
    data class Confirm(val message: String, val onConfirm: () -> Unit) : Overlay()
    object TapManager : Overlay()
    data class Progress(val title: String, val lines: List<String>) : Overlay()
}
```

`brew list --json=v2` returns `{ "formulae": [...], "casks": [...] }`. Both arrays parsed into `List<Package>`, merged and sorted by name. DEPS tab for casks shows "No dependencies."

---

## Services

### BrewService

```kotlin
interface BrewService {
    suspend fun listInstalled(): Result<List<Package>>
    suspend fun search(query: String): Result<List<Package>>
    suspend fun info(name: String, type: PackageType): Result<Package>
    suspend fun install(name: String, type: PackageType): Flow<String>
    suspend fun upgrade(name: String, type: PackageType): Flow<String>
    suspend fun upgradeAll(): Flow<String>
    suspend fun uninstall(name: String, type: PackageType): Flow<String>
    suspend fun pin(name: String): Result<Unit>
    suspend fun unpin(name: String): Result<Unit>
    suspend fun listTaps(): Result<List<String>>
    suspend fun addTap(tap: String): Flow<String>
    suspend fun removeTap(tap: String): Result<Unit>
}
```

`BrewServiceImpl` uses `ProcessBuilder`. Streaming ops return `Flow<String>` — each stdout line emitted as it arrives. Non-streaming ops return `Result<T>` via `runCatching`.

### TldrService

```kotlin
class TldrService {
    private val cache = mutableMapOf<String, String?>()
    suspend fun get(name: String): String?  // null = no page or tldr not installed
}
```

Runs `tldr <name>`, caches per session (including null). No exceptions propagate — silent fallback to `brew desc`.

### Detail Loading

- **INSTALLED mode**: `brew list --json=v2` already includes full info — no extra call on selection.
- **SEARCH mode**: `brew info --json=v2 <name>` fires on selection. `tldr` fetched lazily on first TLDR tab view.

---

## AppViewModel

```kotlin
class AppViewModel(
    private val brewService: BrewService,
    private val tldrService: TldrService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private fun update(block: AppState.() -> AppState) = _state.update(block)
}
```

Actions (all suspend, mutate via `update {}`):

| Action | Behaviour |
|---|---|
| `loadInstalled()` | `loading=true` → `listInstalled()` → set `packages` → clear loading |
| `search(query)` | Same pattern with `search()` |
| `selectPackage(index)` | Updates `selected`; in SEARCH mode fires `info()` if needed |
| `loadTldr(pkg)` | Calls `tldrService.get()`, patches package in state |
| `installPackage(pkg)` | Opens Progress overlay, collects `Flow<String>` lines |
| `uninstallPackage(pkg)` | Confirm overlay → on confirm, Progress overlay |
| `upgradePackage(pkg)` | Progress overlay |
| `upgradeAll()` | Confirm overlay → Progress overlay |
| `togglePin(pkg)` | `pin()`/`unpin()` → refresh list |
| `setStatusMessage(msg)` | Sets message, coroutine clears after 3s |
| `toggleMode()` | Switches INSTALLED ↔ SEARCH, reloads if needed |

Single `MutableStateFlow<AppState>` — no shared mutable state outside it. All actions run in `scope`, cancelled on app exit.

---

## UI Layer

### Lanterna Setup

```
DefaultTerminalFactory().createScreen()
  → MultiWindowTextGUI
  → BreuiWindow (full-screen BasicWindow, BorderLayout)
      ├── CENTER: SplitPanel
      │     ├── LEFT (40%): ListPanel
      │     └── RIGHT (60%): DetailPanel
      └── BOTTOM: StatusBar
```

### ListPanel

Custom `AbstractListBox` showing `name  version  [cask]` rows. Arrow keys move selection, fires `viewModel.selectPackage(index)` on change. Header shows current mode. Search input field (visible when `/` pressed, hidden on `Esc`).

### DetailPanel

Tab bar renders `[Info] [Deps] [tldr]` with active tab highlighted. Body redraws on `detailTab` or `selected` change. Scrollable text area for content.

### StatusBar

Single `Label` at bottom. Reads `state.statusMessage`.

### Overlays

Rendered as modal `BasicWindow` on `MultiWindowTextGUI`. `Esc` closes.

- **Confirm**: message + yes/no buttons
- **Progress**: append-only list streaming stdout lines
- **TapManager**: tap list + text input for adding new tap

### Event Loop

`App.kt` collects `viewModel.state` in a coroutine, calls `gui.updateScreen()` on each emission. Key events intercepted in `WindowListener.onUnhandledInput()`, dispatched to viewModel actions.

### Keyboard Bindings

| Key | Action |
|---|---|
| `Tab` | Toggle INSTALLED ↔ SEARCH |
| `/` | Focus search input |
| `↑` / `↓` | Navigate list |
| `←` / `→` | Switch detail tab |
| `Enter` | Select / execute search |
| `i` | Install selected |
| `u` | Upgrade selected |
| `U` | Upgrade all (confirm) |
| `x` | Uninstall selected (confirm) |
| `p` | Pin / unpin selected |
| `t` | Open Tap Management overlay |
| `r` | Refresh list |
| `q` | Quit |

---

## Error Handling

- All `BrewService` subprocess calls wrapped in `runCatching` → `Result<T>`
- Failures surface as `statusMessage` in `AppState`, auto-cleared after 3s
- `tldr` failures silent — falls back to `brew desc` output in TLDR tab
- Long-running operations stream stdout into Progress overlay

---

## Testing

| Layer | Approach |
|---|---|
| `BrewService` | `FakeBrewService` implements interface, returns fixture JSON. Tests: sorted list, filtered search, `Result.failure` on error |
| `AppViewModel` | `FakeBrewService` + `TestScope`. Asserts: loading flag, packages populated, status auto-clear, overlay lifecycle |
| `TldrService` | Unit test; real `tldr` call skipped if not installed |
| Integration smoke | `@Tag("integration")` — real `brew info git --json=v2`, asserts `name == "git"`, version non-empty |
| UI | Not tested (Lanterna headless not practical) |

Dependencies: `kotlin-test`, `kotlinx-coroutines-test`, `mockk`.

---

## Build Order (Vertical Slices)

1. Gradle project + Lanterna shell + hardcoded list renders
2. `BrewService` + INSTALLED mode fully working
3. Detail panel — Info tab
4. SEARCH mode
5. Overlays (Confirm, Progress, TapManager)
6. Deps tab, tldr tab, pin/upgrade/uninstall actions
