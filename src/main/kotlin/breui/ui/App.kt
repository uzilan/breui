package breui.ui

import breui.model.AppState
import breui.model.DetailTab
import breui.model.Mode
import breui.model.Overlay
import breui.ui.overlays.ConfirmOverlay
import breui.ui.overlays.HelpOverlay
import breui.ui.overlays.ProgressOverlay
import breui.ui.overlays.TapManagerOverlay
import breui.viewmodel.AppViewModel
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.bundle.LanternaThemes
import com.googlecode.lanterna.gui2.AbstractListBox
import com.googlecode.lanterna.gui2.ActionListBox
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.TextGUIGraphics
import com.googlecode.lanterna.gui2.BorderLayout
import com.googlecode.lanterna.gui2.Borders
import com.googlecode.lanterna.gui2.Direction
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.LinearLayout
import com.googlecode.lanterna.gui2.MultiWindowTextGUI
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.WindowListenerAdapter
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class App(
    private val gui: MultiWindowTextGUI,
    private val screen: Screen,
    private val viewModel: AppViewModel,
    private val scope: CoroutineScope
) {
    private val listPanel = ListPanel()
    private val detailPanel = DetailPanel()
    private val statusBar = StatusBar()
    private val window = BasicWindow("breui")
    private var currentThemeName = "businessmachine"

    fun run() {
        window.setHints(setOf(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS))

        val root = Panel(BorderLayout())
        root.addComponent(
            listPanel.withBorder(Borders.singleLine("Packages")),
            BorderLayout.Location.LEFT
        )
        root.addComponent(
            detailPanel.withBorder(Borders.singleLine("Details")),
            BorderLayout.Location.CENTER
        )
        val bottomPanel = Panel(LinearLayout(Direction.VERTICAL))
        bottomPanel.addComponent(statusBar)
        bottomPanel.addComponent(
            Label("  ['] search  [r] refresh  [i] install  [u] upgrade  [U] all  [x] uninstall  [t] theme  [h] help  [←][→] tabs  [q] quit")
        )
        root.addComponent(bottomPanel, BorderLayout.Location.BOTTOM)
        window.component = root

        window.addWindowListener(object : WindowListenerAdapter() {
            override fun onUnhandledInput(
                basePane: Window,
                keyStroke: KeyStroke,
                hasBeenHandled: AtomicBoolean
            ) {
                handleKey(keyStroke)
                hasBeenHandled.set(true)
            }
        })

        scope.launch {
            viewModel.state.collect { state ->
                synchronized(gui) {
                    applyState(state)
                    try { gui.updateScreen() } catch (_: Exception) {}
                }
            }
        }

        listPanel.onSearchKey = {
            if (viewModel.state.value.mode == Mode.SEARCH) {
                viewModel.toggleMode()
            } else {
                viewModel.toggleMode()
                listPanel.activateSearch()
            }
        }
        listPanel.onSearchSubmit = { query -> viewModel.search(query) }

        gui.addWindowAndWait(window)
    }

    private var currentOverlayWindow: BasicWindow? = null
    private var spinnerJob: Job? = null
    private var spinnerIndex = 0
    private val spinnerFrames = arrayOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")

    private fun applyState(state: AppState) {
        listPanel.applyState(state) { index -> viewModel.selectPackage(index) }
        detailPanel.applyState(state)
        if (state.loading || state.overlay is Overlay.Progress) {
            if (spinnerJob == null) {
                spinnerJob = scope.launch {
                    while (true) {
                        synchronized(gui) {
                            statusBar.setText("${spinnerFrames[spinnerIndex % spinnerFrames.size]} Loading...")
                            spinnerIndex++
                            try { gui.updateScreen() } catch (_: Exception) {}
                        }
                        delay(100)
                    }
                }
            }
        } else {
            spinnerJob?.cancel()
            spinnerJob = null
            statusBar.setText(state.statusMessage)
        }
        renderOverlay(state)
    }

    private fun renderOverlay(state: AppState) {
        when (val overlay = state.overlay) {
            null -> {
                if (currentOverlayWindow != null) {
                    currentOverlayWindow?.close()
                    currentOverlayWindow = null
                    screen.clear()
                }
            }
            is Overlay.Confirm -> {
                if (currentOverlayWindow is ConfirmOverlay) return
                currentOverlayWindow?.close()
                currentOverlayWindow = null
                val win = ConfirmOverlay(
                    message = overlay.message,
                    onConfirm = { overlay.onConfirm(); viewModel.closeOverlay() },
                    onDismiss = { viewModel.closeOverlay() }
                )
                currentOverlayWindow = win
                gui.addWindow(win)
            }
            is Overlay.Progress -> {
                val win = currentOverlayWindow as? ProgressOverlay
                    ?: ProgressOverlay(overlay.title).also {
                        currentOverlayWindow = it
                        gui.addWindow(it)
                    }
                for (i in win.lineCount until overlay.lines.size) {
                    win.appendLine(overlay.lines[i])
                }
            }
            is Overlay.TapManager -> {
                if (currentOverlayWindow is TapManagerOverlay) return
                currentOverlayWindow?.close()
                currentOverlayWindow = null
                val win = TapManagerOverlay(
                    taps = state.taps,
                    onAdd = { tap -> viewModel.addTap(tap) },
                    onRemove = { tap -> viewModel.removeTap(tap) },
                    onDismiss = { viewModel.closeOverlay() }
                )
                currentOverlayWindow = win
                gui.addWindow(win)
            }
            is Overlay.Help -> {
                if (currentOverlayWindow is HelpOverlay) return
                currentOverlayWindow?.close()
                currentOverlayWindow = null
                val win = HelpOverlay(onDismiss = { viewModel.closeOverlay() })
                currentOverlayWindow = win
                gui.addWindow(win)
            }
        }
    }

    private fun handleKey(key: KeyStroke) {
        if (listPanel.feedSearchKey(key)) return
        val state = viewModel.state.value
        when {
            key.keyType == KeyType.Character && key.character == 'q' && !listPanel.searchFocused ->
                window.close()
            key.keyType == KeyType.Character && key.character == 'r' && !listPanel.searchFocused ->
                viewModel.backgroundUpdate()
            key.keyType == KeyType.Character && key.character == '\'' && !listPanel.searchFocused ->
                listPanel.onSearchKey?.invoke()
            key.keyType == KeyType.Escape && !listPanel.searchFocused -> {
                viewModel.closeOverlay()
            }
            key.keyType == KeyType.Character && key.character == 'x' && !listPanel.searchFocused -> {
                viewModel.uninstallPackage(viewModel.state.value.selected)
            }
            key.keyType == KeyType.Character && key.character == 'i' && !listPanel.searchFocused ->
                viewModel.installPackage(viewModel.state.value.selected)
            key.keyType == KeyType.Character && key.character == 'u' && !listPanel.searchFocused ->
                viewModel.upgradePackage(viewModel.state.value.selected)
            key.keyType == KeyType.Character && key.character == 'U' && !listPanel.searchFocused ->
                viewModel.upgradeAll()
            key.keyType == KeyType.Character && key.character == 't' && !listPanel.searchFocused ->
                openThemeChooser()
            key.keyType == KeyType.Character && key.character == 'h' && !listPanel.searchFocused ->
                viewModel.openHelp()
            key.keyType == KeyType.ArrowLeft && !listPanel.searchFocused -> {
                val prev = DetailTab.entries[(state.detailTab.ordinal - 1 + 3) % 3]
                viewModel.setDetailTab(prev)
            }
            key.keyType == KeyType.ArrowRight && !listPanel.searchFocused -> {
                val next = DetailTab.entries[(state.detailTab.ordinal + 1) % 3]
                viewModel.setDetailTab(next)
            }
            else -> {}
        }
    }

    private fun openThemeChooser() {
        val themes = LanternaThemes.getRegisteredThemes().sorted()
        val themeMap = themes.associateWith { LanternaThemes.getRegisteredTheme(it) }
        val win = BasicWindow("Choose Theme")
        win.setHints(setOf(Window.Hint.CENTERED))
        val listBox = ActionListBox()
        listBox.setListItemRenderer(object : AbstractListBox.ListItemRenderer<Runnable, ActionListBox>() {
            override fun drawItem(graphics: TextGUIGraphics, lb: ActionListBox, index: Int, item: Runnable, selected: Boolean, focused: Boolean) {
                val name = getLabel(lb, index, item)
                val prefix = if (name == currentThemeName) "> " else "  "
                val label = "$prefix$name"
                val width = graphics.size.columns
                val text = label.take(width).padEnd(width)
                if (selected && focused) {
                    graphics.setForegroundColor(TextColor.ANSI.BLACK)
                    graphics.setBackgroundColor(TextColor.ANSI.GREEN)
                    graphics.putString(0, 0, text)
                } else {
                    val theme = themeMap[name]
                    if (theme != null) {
                        val savedTheme = gui.theme
                        try {
                            gui.theme = theme
                            super.drawItem(graphics, lb, index, item, false, focused)
                        } finally {
                            gui.theme = savedTheme
                        }
                    } else {
                        super.drawItem(graphics, lb, index, item, selected, focused)
                    }
                    graphics.putString(0, 0, text)
                }
            }
        })
        themes.forEach { name ->
            listBox.addItem(name) {
                currentThemeName = name
                gui.setTheme(themeMap[name]!!)
                win.close()
            }
        }
        win.addWindowListener(object : WindowListenerAdapter() {
            override fun onUnhandledInput(basePane: Window, key: KeyStroke, hasBeenHandled: AtomicBoolean) {
                if (key.keyType == KeyType.Escape) { win.close(); hasBeenHandled.set(true) }
            }
        })
        win.component = listBox
        gui.addWindow(win)
    }
}
