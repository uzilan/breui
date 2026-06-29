package breui.ui

import breui.model.AppState
import breui.model.DetailTab
import breui.model.Mode
import breui.model.Overlay
import breui.ui.overlays.ConfirmOverlay
import breui.viewmodel.AppViewModel
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.gui2.BorderLayout
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.Screen
import kotlinx.coroutines.CoroutineScope
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
        root.addComponent(statusBar, BorderLayout.Location.BOTTOM)
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

        gui.addWindowAndWait(window)
    }

    private var currentOverlayWindow: BasicWindow? = null

    private fun applyState(state: AppState) {
        listPanel.applyState(state) { index -> viewModel.selectPackage(index) }
        detailPanel.applyState(state)
        statusBar.setText(state.statusMessage)
        renderOverlay(state)
    }

    private fun renderOverlay(state: AppState) {
        if (state.overlay == null) {
            currentOverlayWindow?.close()
            currentOverlayWindow = null
            return
        }
        if (currentOverlayWindow != null) return // already showing

        val overlayWindow = when (val overlay = state.overlay) {
            is Overlay.Confirm -> ConfirmOverlay(
                message = overlay.message,
                onConfirm = { overlay.onConfirm(); viewModel.closeOverlay() },
                onDismiss = { viewModel.closeOverlay() }
            )
            else -> return // other overlays in later tasks
        }
        currentOverlayWindow = overlayWindow
        gui.addWindow(overlayWindow)
    }

    private fun handleKey(key: KeyStroke) {
        val state = viewModel.state.value
        when {
            key.keyType == KeyType.Character && key.character == 'q' && !listPanel.searchFocused ->
                window.close()
            key.keyType == KeyType.Tab && !listPanel.searchFocused ->
                viewModel.toggleMode()
            key.keyType == KeyType.Character && key.character == 'r' && !listPanel.searchFocused ->
                viewModel.loadInstalled()
            key.keyType == KeyType.Character && key.character == '/' && state.mode == Mode.SEARCH -> {
                listPanel.searchFocused = true
                listPanel.searchBuffer = ""
            }
            listPanel.searchFocused && key.keyType == KeyType.Enter -> {
                listPanel.searchFocused = false
                viewModel.search(listPanel.searchBuffer)
            }
            listPanel.searchFocused && key.keyType == KeyType.Escape -> {
                listPanel.searchFocused = false
                listPanel.searchBuffer = ""
            }
            key.keyType == KeyType.Escape && !listPanel.searchFocused -> {
                viewModel.closeOverlay()
            }
            key.keyType == KeyType.Character && key.character == 'x' && !listPanel.searchFocused -> {
                viewModel.uninstallPackage(viewModel.state.value.selected)
            }
            listPanel.searchFocused && key.keyType == KeyType.Backspace -> {
                if (listPanel.searchBuffer.isNotEmpty()) {
                    listPanel.searchBuffer = listPanel.searchBuffer.dropLast(1)
                }
            }
            listPanel.searchFocused && key.keyType == KeyType.Character -> {
                listPanel.searchBuffer += key.character
            }
            !listPanel.searchFocused && (key.keyType == KeyType.ArrowLeft || (key.keyType == KeyType.Character && key.character == '[')) -> {
                val prev = DetailTab.values()[(state.detailTab.ordinal - 1 + 3) % 3]
                viewModel.setDetailTab(prev)
            }
            !listPanel.searchFocused && (key.keyType == KeyType.ArrowRight || (key.keyType == KeyType.Character && key.character == ']')) -> {
                val next = DetailTab.values()[(state.detailTab.ordinal + 1) % 3]
                viewModel.setDetailTab(next)
            }
            else -> {}
        }
    }
}
