package breui.ui

import breui.model.AppState
import breui.model.DetailTab
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

    private fun applyState(state: AppState) {
        listPanel.applyState(state) { index -> viewModel.selectPackage(index) }
        detailPanel.applyState(state)
        statusBar.setText(state.statusMessage)
    }

    private fun handleKey(key: KeyStroke) {
        val state = viewModel.state.value
        when {
            key.keyType == KeyType.Character && key.character == 'q' -> window.close()
            key.keyType == KeyType.Tab -> viewModel.toggleMode()
            key.keyType == KeyType.Character && key.character == 'r' -> viewModel.loadInstalled()
            key.keyType == KeyType.ArrowLeft || (key.keyType == KeyType.Character && key.character == '[') -> {
                val prev = DetailTab.values()[(state.detailTab.ordinal - 1 + 3) % 3]
                viewModel.setDetailTab(prev)
            }
            key.keyType == KeyType.ArrowRight || (key.keyType == KeyType.Character && key.character == ']') -> {
                val next = DetailTab.values()[(state.detailTab.ordinal + 1) % 3]
                viewModel.setDetailTab(next)
            }
            else -> {}
        }
    }
}
