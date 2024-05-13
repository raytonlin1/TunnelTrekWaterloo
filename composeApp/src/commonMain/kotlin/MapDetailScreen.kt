
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import map.MapMetadata
import map.MapScreenModel
import map.MapsProvider
import ovh.plrapps.mapcompose.ui.MapUI

class MapDetailScreen(
    @Transient private val provider: MapsProvider,
    private val mapMeta: MapMetadata
) : Screen {
    @Composable
    private fun MapNavigationBar(model: MapScreenModel) {
        Row (modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            val coroutineScope = rememberCoroutineScope()

            SearchableDropDownMenu(
                modifier=Modifier.weight(1f),
                placeholder="Source",
                isError = model.srcErr,
                listOfItems = model.mapPoints,
                selectedOptionText = model.sourceString,
                onValueChange = { model.sourceString = it },
                onDropDownItemSelected = { model.sourceString = it }
            )
            SearchableDropDownMenu(
                modifier=Modifier.weight(1f),
                placeholder="Destination",
                isError = model.dstErr,
                listOfItems = model.mapPoints,
                selectedOptionText = model.destinationString,
                onValueChange = { model.destinationString = it },
                onDropDownItemSelected = { model.destinationString = it }
            )
            Button(
                onClick = {
                    val srcPoint = model.getSourceVertex()
                    val dstPoint = model.getDestinationVertex()
                    if (srcPoint == null) {
                        model.srcErr = true
                    }
                    if (dstPoint == null) {
                        model.dstErr = true
                    }
                    if (srcPoint != null && dstPoint != null){
                        model.srcErr = false
                        model.dstErr = false
                        coroutineScope.launch {
                            model.uiState.selectedMarkers.clear()
                            model.uiState.selectedMarkers.addAll(listOf(srcPoint, dstPoint).map { it.id })
                            model.findRoutes(srcPoint, dstPoint)
                            model.uiState.selectRoute(0, false)
                        }
                    }
                },
                modifier = Modifier.height(50.dp).weight(0.5f)
            ) { Text("Start") }
        }
    }

    @Composable
    private fun DirectionsList(model: MapScreenModel){
        AnimatedVisibility(visible = !model.uiState.directionsListCollapsed) {
            LazyColumn {
                items(model.uiState.activeDirections.size) { index ->
                    Row(
                        modifier = Modifier
                            .background(Color.Transparent)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = model.uiState.activeDirections[index],
                            modifier = Modifier
                                .padding(16.dp)
                                .padding(8.dp) // Additional padding for visual effect
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MapSelectionList(model: MapScreenModel) {
        val coroutineScope = rememberCoroutineScope()
        AnimatedVisibility(visible = !model.uiState.routeSelectionCollapsed) {
            LazyColumn {
                val count = model.uiState.activeRoutes.size
                if (count == 0) { item { Text("No routes found") } }
                items(count) { index ->
                    // Change the background or text color based on selection
                    val entryColor = if (index == model.uiState.selectedRoute) {
                        Color.LightGray
                    } else {
                        Color.Transparent
                    }
                    Column (
                        modifier = Modifier
                            .background(entryColor)
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    model.uiState.selectRoute(index)
                                }
                            }
                    ) {
                        Text(
                            text = "Route " + (index + 1).toString(),
                            modifier = Modifier
                                .padding(16.dp)
                                .padding(8.dp) // Additional padding for visual effect
                        )

                        Spacer(Modifier.width(5.dp))
                        val timeEst = model.uiState.activeRoutes[index].totalTime
                        val distEst = model.uiState.activeRoutes[index].totalDistanceMeters
                        Text(
                            text ="Time estimate: " + timeEst.toMinutes() + "min",
                            modifier = Modifier.padding(24.dp)
                        )
                        Spacer(Modifier.width(25.dp))
                        Text(
                            text ="Distance estimate: ${distEst.toInt()} m",
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    override fun Content() =
        ScreenLayout("Map: ${mapMeta.name}") {
            val model = rememberScreenModel { MapScreenModel(provider, mapMeta) }
            LifecycleEffect(
                onStarted = { model.loadMap() },
                onDisposed = {  }
            )

            var isSubMenuExpanded by remember { mutableStateOf(false) }
            val navigator = LocalNavigator.currentOrThrow
            val subMenuItems = listOf(
                SubMenuItem(Icons.Filled.Menu, "View Paths") {
                    model.uiState.routeSelectionCollapsed = !model.uiState.routeSelectionCollapsed
                },
                SubMenuItem(Icons.Filled.Edit, "Edit Map") {
                    navigator.push(MapEditScreen(model.map, provider, model.uiState))
                },
                SubMenuItem(Icons.Filled.ArrowForward, "View Directions"){
                    model.uiState.directionsListCollapsed = !model.uiState.directionsListCollapsed
                }
            )
            MaterialTheme {
                Scaffold(
                    floatingActionButton = { FloatingMenu(subMenuItems, isSubMenuExpanded) {
                        isSubMenuExpanded = !isSubMenuExpanded
                    } },
                ) {
                    Column {
                        Spacer(Modifier.height(5.dp))
                        MapNavigationBar(model)
                        MapSelectionList(model)
                        DirectionsList(model)
                        Spacer(Modifier.height(5.dp))
                        MapUI(Modifier.weight(1f), state = model.uiState.map)
                    }
                }
            }
        }

    @Composable
    fun SearchableDropDownMenu(
        modifier: Modifier = Modifier,
        enable: Boolean = true,
        readOnly: Boolean = false,
        placeholder: String,
        listOfItems: List<String>,
        selectedOptionText: String,
        openedIcon: ImageVector = Icons.Outlined.KeyboardArrowUp,
        closedIcon: ImageVector = Icons.Outlined.KeyboardArrowDown,
        parentTextFieldCornerRadius: Dp = 12.dp,
        onDropDownItemSelected: (String) -> Unit = {},
        onValueChange: (String) -> Unit = {},
        isError: Boolean = false
    ) {
        var searchedOption by rememberSaveable { mutableStateOf("") }
        var expanded by remember { mutableStateOf(false) }
        var filteredItems = emptyList<String>()

        val itemHeights = remember { mutableStateMapOf<Int, Int>() }
        val baseHeight = 530.dp
        val density = LocalDensity.current
        val dropdownMenuVerticalPadding = 8.dp
        val focusRequester = remember { FocusRequester() }

        val maxHeight = remember(itemHeights.toMap()) {
            if (itemHeights.keys.toSet() != listOfItems.indices.toSet()) {
                // if we don't have all heights calculated yet, return default value
                return@remember baseHeight
            }
            val baseHeightInt = with(density) { baseHeight.toPx().toInt() }
            // top+bottom system padding
            var sum = with(density) { dropdownMenuVerticalPadding.toPx().toInt() } * 2
            for ((_, itemSize) in itemHeights.toSortedMap()) {
                sum += itemSize
                if (sum >= baseHeightInt) {
                    return@remember with(density) { (sum - itemSize / 2).toDp() }
                }
            }
            // all items fit into base height
            baseHeight
        }

        Column(
            modifier = modifier
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .clickable {
                        expanded = !expanded
                    },
                value = selectedOptionText,
                readOnly = readOnly,
                enabled = enable,
                onValueChange = onValueChange,
                placeholder = {
                    Text(text = placeholder)
                },
                trailingIcon = {
                    IconToggleButton(
                        checked = expanded,
                        onCheckedChange = {
                            expanded = it
                        }
                    ) {
                        if (expanded) Icon(
                            imageVector = openedIcon,
                            contentDescription = null
                        ) else Icon(
                            imageVector = closedIcon,
                            contentDescription = null
                        )
                    }
                },
                shape = RoundedCornerShape(parentTextFieldCornerRadius),
                isError = isError
            )
            if (expanded) {
                DropdownMenu(
                    modifier = Modifier
                        .requiredSizeIn(maxHeight = maxHeight),
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp).focusRequester(focusRequester),
                            value = searchedOption,
                            onValueChange = { selectedSport ->
                                searchedOption = selectedSport
                                filteredItems = listOfItems.filter {
                                    it.contains(
                                        searchedOption,
                                        ignoreCase = true
                                    )
                                }.toMutableList()
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                            },
                            placeholder = {
                                Text(text = "Search")
                            }
                        )

                        val items = filteredItems.ifEmpty { listOfItems }

                        items.forEach { selectedItem ->
                            DropdownMenuItem(
                                onClick = {
                                    onDropDownItemSelected(selectedItem)
                                    searchedOption = ""
                                    expanded = false
                                }
                            ) {
                                Text(selectedItem)
                            }
                        }
                    }
                }
            }
        }
    }
}