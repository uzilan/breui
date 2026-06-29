package breui.ui

import breui.model.AppState
import breui.model.Mode
import breui.model.Package
import breui.model.PackageType
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.*

class ListPanel : Panel(LinearLayout(Direction.VERTICAL)) {
    private val header = Label("")
    private val listBox = ActionListBox(TerminalSize(40, 0))
    private val searchLabel = Label("")

    init {
        addComponent(header)
        addComponent(listBox)
        addComponent(searchLabel)
        preferredSize = TerminalSize(42, 0)
    }

    fun applyState(state: AppState, onSelect: (Int) -> Unit) {
        header.text = "  [mode: ${state.mode}]${if (state.loading) " loading..." else ""}"
        searchLabel.text = if (state.mode == Mode.SEARCH) "  Query: ${state.searchQuery}" else ""

        listBox.clearItems()
        state.packages.forEachIndexed { index, pkg ->
            listBox.addItem(formatPackage(pkg)) { onSelect(index) }
        }
        if (state.packages.isNotEmpty()) {
            listBox.selectedIndex = state.selected.coerceIn(0, state.packages.size - 1)
        }
    }

    private fun formatPackage(pkg: Package): String {
        val name = pkg.name.take(22).padEnd(22)
        val ver = pkg.version.take(12).padEnd(12)
        val tag = if (pkg.type == PackageType.CASK) "[c]" else "   "
        val outdated = if (pkg.outdated) "*" else " "
        return "$outdated$name $ver$tag"
    }
}
