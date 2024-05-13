import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cafe.adriel.voyager.navigator.Navigator
import ovh.plrapps.mapcompose.utils.DpConverter

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Multiplatform Demo") {
        DpConverter.density = LocalDensity.current.density
        Navigator(screen = HomeScreen())
    }
}
