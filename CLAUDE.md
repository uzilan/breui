# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

**breui** — terminal-based Homebrew management UI for macOS. Kotlin + Lanterna TUI, comparable to `btop` or `lazygit`. Shells out to `brew` CLI; never screen-scrapes (uses `--json=v2` flags).

## Workflow

Run `./gradlew installDist` after every code change.

## Commands

```bash
# Build fat JAR
./gradlew shadowJar          # output: build/libs/breui.jar

# Run
java -jar build/libs/breui.jar

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "breui.viewmodel.AppViewModelTest"

# Run a single test method
./gradlew test --tests "breui.viewmodel.AppViewModelTest.loadInstalled sets packages and clears loading"
```

## Architecture

Unidirectional data flow: `keypress → AppViewModel action → AppState (StateFlow) → UI rerender`

**Layers:**

- `model/` — `AppState`, `Package`, `Overlay` (sealed class), `Mode`/`DetailTab` enums. Pure data, no logic.
- `service/BrewService` — interface; all methods are either `suspend fun … Result<T>` (one-shot) or `fun … Flow<String>` (streaming stdout). `BrewServiceImpl` shells out to `brew`. `TldrService` shells out to `tldr` and caches results in memory.
- `viewmodel/AppViewModel` — owns a single `MutableStateFlow<AppState>`. All mutations go through `update { copy(…) }`. Long-running operations open a `Progress` overlay and stream lines into it via `appendProgressLine`.
- `ui/App` — Lanterna `MultiWindowTextGUI` wiring. Collects `viewModel.state` in a coroutine; calls `applyState` + `gui.updateScreen()` under `synchronized(gui)`. Handles all key events via `WindowListenerAdapter`.
- `ui/ListPanel` — renders package list with highlighting: green (selected), cyan (dependencies of selected), yellow (packages depending on selected). Computes dependents by scanning all packages' dependency lists.
- `ui/DetailPanel` — renders Info/Deps/TLDR tabs. Deps tab shows both dependencies and reverse dependencies ("Required by").
- `ui/StatusBar` — shows status messages.
- `ui/overlays/` — `ConfirmOverlay`, `ProgressOverlay`, `TapManagerOverlay`, `HelpOverlay`; each is a `BasicWindow` shown/closed by `App.renderOverlay`.

**Key invariant:** `App.renderOverlay` is the only place overlays open or close. It diffs `state.overlay` type against `currentOverlayWindow` type to avoid redundant window churn.

## Testing

- `FakeBrewService` provides fixture packages; use it in `AppViewModel` tests via `runTest`.
- `AppViewModelTest` uses `advanceUntilIdle()` / `advanceTimeBy()` from `kotlinx-coroutines-test`.
- UI layer (Lanterna panels) is not tested — headless testing isn't practical with Lanterna.
- One integration smoke test (`BrewServiceImplTest`) runs real `brew info git --json=v2`; requires Homebrew installed.

## PackageType

Packages have a `PackageType` (formula vs cask). `BrewService` methods accept it to route to the right `brew` subcommand. Casks cannot be pinned — `AppViewModel.togglePin` guards this.

## Dependency Highlighting

- **ListPanel** computes two sets on each state update:
  - `dependencyNames` — packages the selected package depends on (cyan highlight).
  - `dependentNames` — packages that depend on the selected package (yellow highlight).
- **DetailPanel DEPS tab** shows both directions:
  - "Dependencies:" lists what the selected package needs.
  - "Required by:" lists packages that depend on the selected package.
- Only formulas have dependencies; casks always have empty dependencies and no "Required by".

## Overlays

New `Overlay.Help` shows a centered help screen with shortcuts, tab descriptions, and highlighting explanation. Triggered by `h` key. Dismisses on any key.
