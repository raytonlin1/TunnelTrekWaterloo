import map.DirectoryMapsProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class MapProviderTest {
    @Test
    fun ListMaps_Returns_ListOf_AvailableMaps() {
        assertEquals(listOf(mapMeta), DirectoryMapsProvider(resourcesDir).listMaps())
    }
}