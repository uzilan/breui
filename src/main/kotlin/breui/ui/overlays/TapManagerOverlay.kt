package breui.ui.overlays

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.ActionListBox
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Direction
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.LinearLayout
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextBox
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.WindowListenerAdapter
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import java.util.concurrent.atomic.AtomicBoolean

class TapManagerOverlay(
    taps: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) : BasicWindow("Tap Manager") {

    private val tapList = ActionListBox(TerminalSize(50, 10))
    private val inputField = TextBox(TerminalSize(40, 1))

    init {
        setHints(setOf(Window.Hint.CENTERED))

        val panel = Panel(LinearLayout(Direction.VERTICAL))
        panel.addComponent(Label("Current taps (x to remove selected):"))

        taps.forEach { tap ->
            tapList.addItem(tap) { /* selection handled by key listener */ }
        }
        panel.addComponent(tapList)
        panel.addComponent(Label(""))
        panel.addComponent(Label("Add tap (Enter to confirm):"))
        panel.addComponent(inputField)

        component = panel

        addWindowListener(object : WindowListenerAdapter() {
            override fun onUnhandledInput(
                basePane: Window,
                keyStroke: KeyStroke,
                hasBeenHandled: AtomicBoolean
            ) {
                when {
                    keyStroke.keyType == KeyType.Escape -> {
                        close()
                        onDismiss()
                        hasBeenHandled.set(true)
                    }
                    keyStroke.keyType == KeyType.Enter && inputField.text.isNotBlank() -> {
                        val tap = inputField.text.trim()
                        close()
                        onAdd(tap)
                        hasBeenHandled.set(true)
                    }
                    keyStroke.keyType == KeyType.Character && keyStroke.character == 'x' -> {
                        val selected = taps.getOrNull(tapList.selectedIndex)
                        if (selected != null) {
                            close()
                            onRemove(selected)
                        }
                        hasBeenHandled.set(true)
                    }
                }
            }
        })
    }
}
