package breui

import breui.service.NoOpTldrService
import breui.service.StubBrewService
import breui.ui.App
import breui.viewmodel.AppViewModel
import com.googlecode.lanterna.gui2.MultiWindowTextGUI
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val terminal = DefaultTerminalFactory().createTerminal()
    val screen = TerminalScreen(terminal)
    screen.startScreen()
    val gui = MultiWindowTextGUI(screen)

    val scope = CoroutineScope(Dispatchers.Default)
    val viewModel = AppViewModel(StubBrewService(), NoOpTldrService(), scope)

    viewModel.loadInstalled()

    App(gui, screen, viewModel, scope).run()

    scope.cancel()
    screen.stopScreen()
}
