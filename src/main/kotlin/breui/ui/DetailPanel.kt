package breui.ui

import breui.model.AppState
import breui.model.DetailTab
import breui.model.Package
import breui.model.PackageType
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
        content.text = if (pkg == null) "No package selected" else renderContent(pkg, state.detailTab, state.packages)
    }

    private fun buildTabBar(active: DetailTab): String {
        return DetailTab.entries.joinToString("  ") { tab ->
            if (tab == active) "[${tab.name}]" else " ${tab.name} "
        }
    }

    private fun renderContent(pkg: Package, tab: DetailTab, allPackages: List<Package>): String = when (tab) {
        DetailTab.INFO -> buildString {
            appendLine("Name:     ${pkg.name}")
            appendLine("Version:  ${pkg.version}")
            appendLine("Type:     ${pkg.type.name.lowercase()}")
            appendLine("Outdated: ${if (pkg.outdated) "yes *" else "no"}")
            appendLine("Pinned:   ${if (pkg.pinned) "yes" else "no"}")
            appendLine()
            appendLine(pkg.desc ?: "(no description)")
            appendLine()
            if (pkg.homepage != null) appendLine("Homepage: ${pkg.homepage}")
            if (pkg.license != null) appendLine("License:  ${pkg.license}")
        }
        DetailTab.DEPS -> {
            val dependencies = pkg.dependencies
            val dependents = allPackages.filter { pkg.name in it.dependencies }.map { it.name }
            buildString {
                if (dependencies.isNotEmpty()) {
                    appendLine("Dependencies:")
                    dependencies.forEach { appendLine("  • $it") }
                } else {
                    if (pkg.type == PackageType.CASK) {
                        appendLine("No dependencies (cask)")
                    } else {
                        appendLine("No dependencies")
                    }
                }
                if (dependents.isNotEmpty()) {
                    appendLine()
                    appendLine("Required by:")
                    dependents.forEach { appendLine("  • $it") }
                }
            }
        }
        DetailTab.TLDR -> pkg.tldr ?: "Loading..."
    }
}
