package breui

import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import java.util.concurrent.atomic.AtomicBoolean

fun main() {
    val terminal = DefaultTerminalFactory().createTerminal()
    val screen = TerminalScreen(terminal)
    screen.startScreen()

    val gui = MultiWindowTextGUI(screen)
    val window = BasicWindow("breui")
    window.setHints(setOf(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS))

    val panel = Panel()
    panel.addComponent(Label("breui — Homebrew UI"))
    panel.addComponent(Label("Press q to quit"))
    window.component = panel

    window.addWindowListener(object : WindowListenerAdapter() {
        override fun onUnhandledInput(
            basePane: Window,
            keyStroke: KeyStroke,
            hasBeenHandled: AtomicBoolean
        ) {
            if (keyStroke.keyType == KeyType.Character && keyStroke.character == 'q') {
                window.close()
                hasBeenHandled.set(true)
            }
        }
    })

    gui.addWindowAndWait(window)
    screen.stopScreen()
}
