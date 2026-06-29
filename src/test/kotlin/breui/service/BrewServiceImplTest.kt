package breui.service

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertTrue

@Tag("integration")
class BrewServiceImplTest {

    @Test
    fun `listInstalled returns packages including git`() = runTest {
        val service = BrewServiceImpl()
        val result = service.listInstalled()
        assertTrue(result.isSuccess, "Expected success but got: ${result.exceptionOrNull()?.message}")
        val packages = result.getOrThrow()
        assertTrue(packages.isNotEmpty(), "Expected at least one installed package")
        assertTrue(packages.all { it.name.isNotBlank() }, "All packages should have a name")
        assertTrue(packages.all { it.version.isNotBlank() }, "All packages should have a version")
        assertTrue(packages == packages.sortedBy { it.name }, "Packages should be sorted by name")
    }
}
