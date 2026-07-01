# breui

A terminal UI for managing Homebrew packages on macOS — browse, search, install, upgrade, and uninstall without leaving the terminal.

![breui screenshot](docs/businessmachine.png)

## Features

- Browse installed formulae and casks
- Search Homebrew and install packages
- Upgrade individual packages or all at once
- Live progress log for install/upgrade operations
- `tldr` integration in the detail panel (falls back to `brew desc`)
- Multiple Lanterna themes (`t` to switch)
- Background `brew update` on launch

## Requirements

- macOS with [Homebrew](https://brew.sh) installed
- Java 25+
- [`tldr`](https://tldr.sh) (optional — for tldr tab content)

## Build & Run

```bash
./gradlew shadowJar
java -jar build/libs/breui.jar
```

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `'` | Toggle Installed ↔ Search mode / focus search |
| `↑` / `↓` | Navigate list |
| `←` / `→` | Switch detail tab (Info / Deps / tldr) |
| `i` | Install selected package |
| `u` | Upgrade selected package |
| `U` | Upgrade all outdated packages |
| `x` | Uninstall selected package |
| `t` | Open theme chooser |
| `r` | Refresh installed list |
| `Esc` | Close overlay |
| `q` | Quit |
