package widgets

import UIMapState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import deleteMarker
import map.MutableCampusMap
import ovh.plrapps.mapcompose.api.removeCallout
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * A callout which animates its entry with an overshoot scaling interpolator.
 */
@Composable
fun Callout(
    x: Double, y: Double,
    title: String
) {
    CalloutContainer {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "position ${df.format(x)} , ${df.format(y)}",
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally)
                    .padding(top = 4.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
        }
    }
}

private val df = DecimalFormat("#.##").apply {
    roundingMode = RoundingMode.CEILING
}

@Composable
private fun CalloutContainer(content: @Composable () -> Unit) =
    Surface(
        Modifier
            .padding(10.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
        shape = RoundedCornerShape(5.dp),
        elevation = 10.dp
    ) { content() }

@Composable
fun EditCallout(
    id: Int,
    mutableMap: MutableCampusMap,
    uiMapState: UIMapState
) {
    val point = mutableMap.points[id]!!
    var name by remember { mutableStateOf(point.name) }
    CalloutContainer {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TextField(
                label = { Text("Name") },
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                    .width(120.dp),
            )
            Text(
                text = "position ${df.format(point.coords.x)} , ${df.format(point.coords.y)}",
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally)
                    .padding(top = 4.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
            Row(horizontalArrangement = Arrangement.Center) {
                Button(onClick = {
                    mutableMap.points[id] = point.copy(
                        name = name
                    )
                    uiMapState.map.removeCallout(id.toString())
                }) { Icon(Icons.Filled.Check, "OK") }
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    uiMapState.map.removeCallout(id.toString())
                }) { Icon(Icons.Filled.Close, "Cancel") }
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    mutableMap.deleteMarker(uiMapState, id.toString())
                    uiMapState.map.removeCallout(id.toString())
                }) { Icon(Icons.Filled.Delete, "Delete") }
            }
        }
    }
}