import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import firebase.downloadMap
import firebase.listMaps
import firebase.uploadMap
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import map.CampusMap
import map.MapsProvider
import map.LocalMapsProvider
import map.MapMetadata

class MapListScreenModel : ScreenModel {
    var maps by mutableStateOf(listOf<MapMetadata>())
}

class MapListScreen : Screen {
    @OptIn(DelicateCoroutinesApi::class)
    @Composable
    override fun Content() {
        val provider: MapsProvider = remember { LocalMapsProvider }
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { MapListScreenModel() }

        LaunchedEffect(Unit) { model.maps = provider.listMaps() }

        Column {
            LazyColumn {
                if (model.maps.isEmpty()) {
                    item { Text("No maps found") }
                }

                items(model.maps) { mapMeta ->
                    Button(
                        onClick = { navigator.push(MapDetailScreen(provider, mapMeta)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Go to map ${mapMeta.name}")
                    }
                }
            }

            if (Firebase.auth.currentUser != null) {
                CloudMapWidget(provider)
            }
        }
    }

    private @Composable
    fun CloudMapWidget(provider: MapsProvider) {
        val model = rememberScreenModel { MapListScreenModel() }
        val useruid = Firebase.auth.currentUser!!.uid
        var mapName by remember { mutableStateOf("Select Map Name") }
        val scope = rememberCoroutineScope()
        var mapNames by remember { mutableStateOf<List<String>>(emptyList()) }

        LifecycleEffect(onStarted = {
            scope.launch { mapNames = listMaps() }
        })

        Button(
            onClick = {
                    GlobalScope.launch {
                        uploadMap(useruid, mapName)
                    }.invokeOnCompletion {
                        println("Upload Complete!")
                    }
            }
        ) {
            Text("Begin Upload")
        }

        Button(
            onClick = {
                if (mapNames.contains(mapName)) {
                    GlobalScope.launch {
                        downloadMap(useruid, mapName)
                    }.invokeOnCompletion {
                        model.maps = provider.listMaps()
                        println("Download Complete")
                    }
                }
            }
        ) {
            Text("Begin Download")
        }
        OutlinedTextField(
            label = { Text("Select Map Name") },
            value = mapName,
            onValueChange = { mapName = it }
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("List of Available Cloud Maps:")
            Button(onClick = {
                scope.launch { mapNames = listMaps() }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "refresh")
            }
        }

        LazyColumn {
            items(mapNames) { item ->
                Button(
                    onClick = {
                        mapName = item
                    }
                ) {
                    Text(text = item)
                }
            }
        }

    }
}