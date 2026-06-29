# breui — Specification

A terminal-based Homebrew management UI for macOS, written in Kotlin. Provides a friendly interactive interface for browsing, searching, and managing Homebrew packages — comparable in feel to `btop` or `lazygit`.

---

## 1. Technology Stack

| Layer | Choice | Rationale |
|---|---|---|
| Language | Kotlin (standard project, Gradle) | Idiomatic, coroutine support |
| TUI framework | Lanterna | Battle-tested full-screen TUI, handles mouse/keyboard/resize |
| Async | Kotlin Coroutines + StateFlow | Non-blocking brew subprocess calls; reactive state |
| Build | Gradle with `.kts` build files | Standard Kotlin project setup |

---

## 2. Layout

```
┌─────────────────────────────────────────────────────┐
│  breui                          [mode: INSTALLED]   │
├──────────────────────┬──────────────────────────────┤
│  List Panel (left)   │  Detail Panel (right)        │
│                      │                              │
│  > git          2.x  │  git                         │
│    curl         8.x  │  ─────────────────           │
│    wget         1.x  │  [Info] [Deps] [tldr]        │
│    ...               │                              │
│                      │  Desc: tldr output here...   │
│  [/] search          │  Version: 2.43.0             │
│  [Tab] toggle mode   │  Deps: openssl, pcre2        │
│  [↑↓] navigate       │                              │
│  [i] install etc.    │                              │
└──────────────────────┴──────────────────────────────┘
```

A persistent status bar at the bottom displays transient messages (errors, confirmations).

---

## 3. Modes

`Tab` toggles between two modes:

- **INSTALLED** — lists all installed packages via `brew list --json=v2`, sorted by name.
- **SEARCH** — shows results of `brew search <query>` with details loaded via `brew info --json=v2` on selection.

---

## 4. Detail Panel Tabs

Switched with left/right arrow or `[ ]`:

- **Info** — name, version, description, homepage, license.
- **Deps** — runtime dependency tree (from `brew info` JSON).
- **tldr** — `tldr` output if a page exists for the package; falls back silently to `brew desc` if `tldr` is not installed or has no page.

---

## 5. Keyboard Bindings

| Key | Action |
|---|---|
| `Tab` | Toggle Installed ↔ Search mode |
| `/` | Focus search input |
| `↑` / `↓` | Navigate list |
| `Enter` | Select / expand |
| `i` | Install selected package |
| `u` | Upgrade selected package |
| `U` | Upgrade all (with confirm dialog) |
| `x` | Uninstall selected (with confirm dialog) |
| `p` | Pin / unpin selected package |
| `t` | Open Tap Management overlay |
| `r` | Refresh list |
| `q` | Quit |

---

## 6. Overlays

Modal overlays, dismissed with `Esc`:

- **Confirm dialog** — required before any destructive action (uninstall, upgrade-all). Shows action description and prompts yes/no.
- **Tap Management** — lists current taps; supports adding a new tap (text input) and removing an existing one.
- **Progress / Log** — streams live `brew` stdout output during install or upgrade operations. Displayed as a scrolling log panel.

---

## 7. Data Model

```kotlin
data class Package(
    val name: String,
    val version: String,
    val installed: Boolean,
    val pinned: Boolean,
    val outdated: Boolean,
    val desc: String,
    val homepage: String,
    val dependencies: List<String>,
    val tldr: String? = null
)

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

sealed class Overlay {
    data class Confirm(val message: String, val onConfirm: () -> Unit) : Overlay()
    object TapManager : Overlay()
    data class Progress(val title: String, val lines: List<String>) : Overlay()
}
```

---

## 8. Architecture

State flows in one direction:

```
user keypress → action → BrewService → new AppState → UI rerender
```

- **AppViewModel** — owns a single `MutableStateFlow<AppState>`. All actions are `suspend` functions that update state atomically. No shared mutable state outside the flow.
- **BrewService** — interface; coroutine-based implementation shells out to `brew` CLI. Uses `brew info --json=v2` and `brew list --json=v2` for structured output (no screen-scraping).
- **TldrService** — shells out to `tldr`; caches results in memory for the session.
- **UI layer** — Lanterna panels that collect and render `AppState`. Redraws on state changes.

---

## 9. Error Handling

- `BrewService` wraps all subprocess calls in `runCatching`, returning `Result<T>`.
- Failures surface as `statusMessage` in `AppState`, shown in the status bar and auto-cleared after 3 seconds.
- If `tldr` is not installed or has no page for a package, the app silently falls back to `brew desc` — no error is shown to the user.
- Long-running operations (install, upgrade) stream `stdout` lines into the Progress overlay so the user can see what Homebrew is doing.

---

## 10. Testing

| Layer | Approach |
|---|---|
| `BrewService` | Interface-backed; unit tests use a fake returning fixture JSON |
| `AppViewModel` | Unit-tested with fake service; asserts correct state transitions per action |
| UI | Not tested (Lanterna headless testing not practical) |
| Integration smoke test | Runs real `brew info git --json=v2`; asserts expected fields are present and parseable |
