package org.example.project

import HomeScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.navigator.Navigator
import firebase.FirebaseResourceManager
import map.DemoResourceMapsProvider
import map.LocalMapsProvider
import ovh.plrapps.mapcompose.utils.DpConverter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DemoResourceMapsProvider.assets = assets
        FirebaseResourceManager.filesDir = filesDir

        setContent {
            DpConverter.density = LocalDensity.current.density
            Navigator(screen = HomeScreen())
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    Navigator(HomeScreen())
}