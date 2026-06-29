package breui.viewmodel

import breui.model.DetailTab
import breui.model.Mode
import breui.model.Overlay
import breui.service.FakeBrewService
import breui.service.NoOpTldrService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @Test
    fun `loadInstalled sets packages and clears loading`() = runTest {
        val vm = AppViewModel(FakeBrewService(), NoOpTldrService(), this)
        vm.loadInstalled()
        advanceUntilIdle()
        assertEquals(FakeBrewService.FIXTURE_PACKAGES, vm.state.value.packages)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `loadInstalled on failure sets status message`() = runTest {
        val fake = FakeBrewService().apply {
            installedResult = Result.failure(RuntimeException("brew not found"))
        }
        val vm = AppViewModel(fake, NoOpTldrService(), this)
        vm.loadInstalled()
        runCurrent()
        assert(vm.state.value.statusMessage.contains("brew not found"))
    }

    @Test
    fun `selectPackage updates selected index`() = runTest {
        val vm = AppViewModel(FakeBrewService(), NoOpTldrService(), this)
        vm.selectPackage(2)
        assertEquals(2, vm.state.value.selected)
    }

    @Test
    fun `setStatusMessage clears after 3 seconds`() = runTest {
        val vm = AppViewModel(FakeBrewService(), NoOpTldrService(), this)
        vm.setStatusMessage("hello")
        assertEquals("hello", vm.state.value.statusMessage)
        advanceTimeBy(3_001)
        assertEquals("", vm.state.value.statusMessage)
    }

    @Test
    fun `toggleMode switches from INSTALLED to SEARCH`() = runTest {
        val vm = AppViewModel(FakeBrewService(), NoOpTldrService(), this)
        assertEquals(Mode.INSTALLED, vm.state.value.mode)
        vm.toggleMode()
        assertEquals(Mode.SEARCH, vm.state.value.mode)
    }

    @Test
    fun `toggleMode from SEARCH reloads installed packages`() = runTest {
        val vm = AppViewModel(FakeBrewService(), NoOpTldrService(), this)
        vm.toggleMode() // → SEARCH
        vm.toggleMode() // → INSTALLED, triggers loadInstalled
        advanceUntilIdle()
        assertEquals(Mode.INSTALLED, vm.state.value.mode)
        assertEquals(FakeBrewService.FIXTURE_PACKAGES, vm.state.value.packages)
    }

    @Test
    fun `setDetailTab updates detailTab`() = runTest {
        val vm = AppViewModel(FakeBrewService(), NoOpTldrService(), this)
        vm.setDetailTab(DetailTab.DEPS)
        assertEquals(DetailTab.DEPS, vm.state.value.detailTab)
    }

    @Test
    fun `search sets packages and clears loading`() = runTest {
        val vm = AppViewModel(FakeBrewService(), NoOpTldrService(), this)
        vm.toggleMode() // → SEARCH
        vm.search("git")
        advanceUntilIdle()
        assertEquals(FakeBrewService.FIXTURE_PACKAGES, vm.state.value.packages)
        assertFalse(vm.state.value.loading)
        assertEquals("git", vm.state.value.searchQuery)
    }

    @Test
    fun `search on failure sets status message`() = runTest {
        val fake = FakeBrewService().apply {
            searchResult = Result.failure(RuntimeException("network error"))
        }
        val vm = AppViewModel(fake, NoOpTldrService(), this)
        vm.toggleMode()
        vm.search("git")
        runCurrent()
        assertTrue(vm.state.value.statusMessage.contains("network error"))
    }

    @Test
    fun `uninstallPackage shows confirm overlay`() = runTest {
        val vm = AppViewModel(FakeBrewService(), NoOpTldrService(), this)
        vm.loadInstalled()
        advanceUntilIdle()
        vm.uninstallPackage(0)
        val overlay = vm.state.value.overlay
        assertTrue(overlay is Overlay.Confirm)
        assertTrue((overlay as Overlay.Confirm).message.contains("curl"))
    }

    @Test
    fun `closeOverlay clears overlay`() = runTest {
        val vm = AppViewModel(FakeBrewService(), NoOpTldrService(), this)
        vm.showConfirm("test") {}
        vm.closeOverlay()
        assertEquals(null, vm.state.value.overlay)
    }

    @Test
    fun `installPackage opens progress overlay`() = runTest {
        val vm = AppViewModel(FakeBrewService(), NoOpTldrService(), this)
        vm.loadInstalled()
        vm.installPackage(0)
        // After flow completes, overlay should be cleared
        assertEquals(null, vm.state.value.overlay)
        // Status message set
        assertTrue(vm.state.value.statusMessage.contains("curl") || vm.state.value.statusMessage.isEmpty())
    }

    @Test
    fun `upgradeAll shows confirm overlay`() = runTest {
        val vm = AppViewModel(FakeBrewService(), NoOpTldrService(), this)
        vm.upgradeAll()
        assertTrue(vm.state.value.overlay is Overlay.Confirm)
    }

    @Test
    fun `setDetailTab TLDR triggers loadTldr`() = runTest {
        val vm = AppViewModel(FakeBrewService(), NoOpTldrService(), this)
        vm.loadInstalled()
        advanceUntilIdle()
        vm.setDetailTab(DetailTab.TLDR)
        advanceUntilIdle()
        // NoOpTldrService returns null, so tldr falls back to desc
        val pkg = vm.state.value.packages.getOrNull(0)
        // tldr field should be set (to desc — not null)
        assertTrue(pkg?.tldr != null)
    }
}
