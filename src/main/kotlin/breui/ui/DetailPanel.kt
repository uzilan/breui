package breui.ui

import breui.model.AppState
import breui.model.DetailTab
import breui.model.Package
import com.googlecode.lanterna.gui2.*

class DetailPanel : Panel(LinearLayout(Direction.VERTICAL)) {
    private val tabBar = Label("")
    private val content = Label("")

    init {
        addComponent(tabBar)
        addComponent(Label("─".repeat(40)))
        addComponent(content)
    }

    fun applyState(state: AppState) {
        val pkg = state.packages.getOrNull(state.selected)
        tabBar.text = buildTabBar(state.detailTab)
        content.text = if (pkg == null) "No package selected" else renderContent(pkg, state.detailTab)
    }

    private fun buildTabBar(active: DetailTab): String {
        return DetailTab.values().joinToString("  ") { tab ->
            if (tab == active) "[${tab.name}]" else " ${tab.name} "
        }
    }

    private fun renderContent(pkg: Package, tab: DetailTab): String = when (tab) {
        DetailTab.INFO -> buildString {
            appendLine("Name:     ${pkg.name}")
            appendLine("Version:  ${pkg.version}")
            appendLine("Type:     ${pkg.type.name.lowercase()}")
            appendLine("Outdated: ${if (pkg.outdated) "yes *" else "no"}")
            appendLine("Pinned:   ${if (pkg.pinned) "yes" else "no"}")
            appendLine()
            appendLine(pkg.desc)
            appendLine()
            appendLine("Homepage: ${pkg.homepage}")
            if (pkg.license != null) appendLine("License:  ${pkg.license}")
        }
        DetailTab.DEPS -> "Loading..."  // Task 10
        DetailTab.TLDR -> "Loading..."  // Task 11
    }
}
