# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

**breui** — terminal-based Homebrew management UI for macOS. Kotlin + Lanterna TUI, comparable to `btop` or `lazygit`. Shells out to `brew` CLI; never screen-scrapes (uses `--json=v2` flags).

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
- `ui/ListPanel`, `ui/DetailPanel`, `ui/StatusBar` — stateless renderers; receive `AppState` and callbacks.
- `ui/overlays/` — `ConfirmOverlay`, `ProgressOverlay`, `TapManagerOverlay`; each is a `BasicWindow` shown/closed by `App.renderOverlay`.

**Key invariant:** `App.renderOverlay` is the only place overlays open or close. It diffs `state.overlay` type against `currentOverlayWindow` type to avoid redundant window churn.

## Testing

- `FakeBrewService` provides fixture packages; use it in `AppViewModel` tests via `runTest`.
- `AppViewModelTest` uses `advanceUntilIdle()` / `advanceTimeBy()` from `kotlinx-coroutines-test`.
- UI layer (Lanterna panels) is not tested — headless testing isn't practical with Lanterna.
- One integration smoke test (`BrewServiceImplTest`) runs real `brew info git --json=v2`; requires Homebrew installed.

## PackageType

Packages have a `PackageType` (formula vs cask). `BrewService` methods accept it to route to the right `brew` subcommand. Casks cannot be pinned — `AppViewModel.togglePin` guards this.
