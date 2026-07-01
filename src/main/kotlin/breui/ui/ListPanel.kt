package breui.ui

import breui.model.AppState
import breui.model.Mode
import breui.model.Package
import breui.model.PackageType
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType

class ListPanel : Panel(BorderLayout()) {
    private val header = Label("")
    private val searchLine = Label("")
    var searchBuffer = ""
    var searchFocused = false

    @Volatile private var onSelectCallback: ((Int) -> Unit)? = null
    @Volatile var onSearchKey: (() -> Unit)? = null
    @Volatile var onSearchSubmit: ((String) -> Unit)? = null
    private var lastPackages: List<Package> = emptyList()
    private var dependencyNames: Set<String> = emptySet()

    private val listBox = object : ActionListBox() {
        override fun handleKeyStroke(key: KeyStroke): Interactable.Result {
            if (feedSearchKey(key)) return Interactable.Result.HANDLED
            if (key.keyType == KeyType.Character && key.character == '\'') {
                onSearchKey?.invoke()
                return Interactable.Result.HANDLED
            }
            val prev = selectedIndex
            val result = super.handleKeyStroke(key)
            if (selectedIndex != prev) onSelectCallback?.invoke(selectedIndex)
            return result
        }
    }

    init {
        listBox.setListItemRenderer(object : AbstractListBox.ListItemRenderer<Runnable, ActionListBox>() {
            override fun drawItem(
                graphics: TextGUIGraphics,
                listBox: ActionListBox,
                index: Int,
                item: Runnable,
                selected: Boolean,
                focused: Boolean
            ) {
                val pkg = lastPackages.getOrNull(index)
                val label = getLabel(listBox, index, item)
                val width = graphics.size.columns
                val text = label.take(width).padEnd(width)
                when {
                    selected && focused -> {
                        graphics.setForegroundColor(TextColor.ANSI.BLACK)
                        graphics.setBackgroundColor(TextColor.ANSI.GREEN)
                        graphics.putString(0, 0, text)
                    }
                    pkg != null && pkg.name in dependencyNames -> {
                        graphics.setForegroundColor(TextColor.ANSI.BLACK)
                        graphics.setBackgroundColor(TextColor.ANSI.CYAN)
                        graphics.putString(0, 0, text)
                    }
                    else -> super.drawItem(graphics, listBox, index, item, selected, focused)
                }
            }
        })
        addComponent(header, BorderLayout.Location.TOP)
        addComponent(listBox, BorderLayout.Location.CENTER)
        addComponent(searchLine, BorderLayout.Location.BOTTOM)
        preferredSize = TerminalSize(42, 0)
    }

    fun feedSearchKey(key: KeyStroke): Boolean {
        if (!searchFocused) return false
        when (key.keyType) {
            KeyType.Character -> {
                if (key.character == '\'') {
                    searchFocused = false
                    searchBuffer = ""
                    onSearchKey?.invoke()
                    return true
                }
                searchBuffer += key.character
                searchLine.text = "  Query: ${searchBuffer}_"
            }
            KeyType.Backspace -> {
                if (searchBuffer.isNotEmpty()) searchBuffer = searchBuffer.dropLast(1)
                searchLine.text = "  Query: ${searchBuffer}_"
            }
            KeyType.Enter -> {
                searchFocused = false
                searchLine.text = "  Query: $searchBuffer"
                if (searchBuffer.isNotEmpty()) onSearchSubmit?.invoke(searchBuffer)
            }
            KeyType.Escape -> {
                searchFocused = false
                searchBuffer = ""
                searchLine.text = ""
            }
            else -> return false
        }
        return true
    }

    fun activateSearch() {
        searchFocused = true
        searchBuffer = ""
        searchLine.text = "  Query: _"
    }

    fun applyState(state: AppState, onSelect: (Int) -> Unit) {
        onSelectCallback = onSelect
        header.text = "  [mode: ${state.mode}]${if (state.loading) " loading..." else ""}"
        if (state.mode != Mode.SEARCH) {
            searchFocused = false
            searchBuffer = ""
            searchLine.text = ""
        } else if (!searchFocused) {
            searchLine.text = "  Query: $searchBuffer"
        }

        dependencyNames = state.packages.getOrNull(state.selected)?.dependencies?.toSet() ?: emptySet()

        if (state.packages != lastPackages) {
            lastPackages = state.packages
            listBox.clearItems()
            state.packages.forEachIndexed { index, pkg ->
                listBox.addItem(formatPackage(pkg)) { onSelect(index) }
            }
        }

        if (state.packages.isNotEmpty()) {
            listBox.selectedIndex = state.selected.coerceIn(0, state.packages.size - 1)
        }
    }

    private fun formatPackage(pkg: Package): String {
        val name = pkg.name.take(22).padEnd(22)
        val ver = pkg.version.take(12).padEnd(12)
        val tag = if (pkg.type == PackageType.CASK) "[c]" else "   "
        val installed = if (pkg.installed) "+" else " "
        val outdated = if (pkg.outdated) "*" else " "
        return "$outdated$installed$name $ver$tag"
    }
}
