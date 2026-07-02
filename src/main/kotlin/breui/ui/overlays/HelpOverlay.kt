package breui.ui.overlays

import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Direction
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.LinearLayout
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.WindowListenerAdapter
import com.googlecode.lanterna.input.KeyType
import java.util.concurrent.atomic.AtomicBoolean

class HelpOverlay(val onDismiss: () -> Unit) : BasicWindow("Help") {
    init {
        setHints(setOf(Window.Hint.CENTERED))
        val panel = Panel(LinearLayout(Direction.VERTICAL))

        val text = """
            ╔═══════════════════════════════════════════╗
            ║         BREUI - HOMEBREW MANAGEMENT       ║
            ╚═══════════════════════════════════════════╝

            SHORTCUTS:
              ['] search       Search for packages
              [r] refresh      Reload installed packages
              [i] install      Install selected package
              [u] upgrade      Upgrade selected package
              [U] all          Upgrade all packages
              [x] uninstall    Uninstall selected package
              [t] theme        Change color theme
              [↑][↓] navigate  Scroll through packages
              [←][→] tabs      Switch detail view tabs
              [ESC] dismiss    Close dialogs
              [q] quit         Exit breui

            DETAIL TABS:
              [INFO]  Package name, version, description
              [DEPS]  Dependencies (only for formulas)
              [TLDR]  Quick command examples (cached)

            THEMES:
              Choose from 6 Lanterna color themes.
              Current theme shown with ">" in theme menu.
              Themes apply instantly.

            DEPENDENCY HIGHLIGHTING:
              In the DEPS tab, dependencies shown in green.
              Dependencies are only available for formulas.
              Casks have no dependencies.

            Press any key to close this help screen.
        """.trimIndent()

        for (line in text.split("\n")) {
            panel.addComponent(Label(line))
        }

        addWindowListener(object : WindowListenerAdapter() {
            override fun onUnhandledInput(basePane: Window, key: com.googlecode.lanterna.input.KeyStroke, hasBeenHandled: AtomicBoolean) {
                onDismiss()
                hasBeenHandled.set(true)
            }
        })

        component = panel
    }
}
