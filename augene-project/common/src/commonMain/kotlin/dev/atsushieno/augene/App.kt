package dev.atsushieno.augene
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

val model
    get() = AugeneModel.instance

@Composable
fun App() {
    MaterialTheme {

        Column(verticalArrangement = Arrangement.Bottom) {

            // Tabs
            val tabIndexSongsAndMappings = 0
            val tabIndexGraphs = 1
            val tabIndexMasterPlugins = 2
            val tabIndexSettings = 3

            var tabIndex by remember { mutableStateOf(0) }

            when (tabIndex) {
                tabIndexSongsAndMappings -> Tab(selected = tabIndex == tabIndexSongsAndMappings,
                    onClick = { tabIndex = tabIndexSongsAndMappings }) { SongAndMappings() }
                tabIndexGraphs ->Tab(selected = tabIndex == tabIndexGraphs,
                    onClick = { tabIndex = tabIndexGraphs }) { AudioGraphList() }
                tabIndexMasterPlugins ->Tab(selected = tabIndex == tabIndexMasterPlugins,
                    onClick = { tabIndex = tabIndexMasterPlugins }) { MasterPluginList() }
                tabIndexSettings ->Tab(selected = tabIndex == tabIndexSettings,
                    onClick = { tabIndex = tabIndexSettings }) { AppSettings() }
            }

            // FAB
            var fabActionMenuState by remember { mutableStateOf(false) }
            if (fabActionMenuState) {
                Button(onClick = { model.processOpenProject() }) { Text("Load") }
                Button(onClick = { model.processSaveProject() }) { Text("Save") }
                Button(onClick = { model.processCompile() }) { Text("Compile") }
                Button(onClick = { model.processPlay() }) { Text("Play") }
            }
            FloatingActionButton(onClick = { fabActionMenuState = !fabActionMenuState }) {
                Text(if (fabActionMenuState) "-" else "+")
            }

            // BottomNavigation
            val navigationItemLabels = listOf("Song", "AudioGraphs", "Master Plugins", "Settings")

            BottomNavigation {
                navigationItemLabels.forEachIndexed { index, l ->
                    BottomNavigationItem(selected = tabIndex == index, onClick = { tabIndex = index}, label = { Text(l) }, icon = {})
                }
            }
        }
    }
}

@Composable
fun SongAndMappings() {
    Row {
        MmlList()
        TrackMappingList()
    }
}

@Composable
fun MmlList() {
    Button(onClick = {}) {
        Text("New MML")
    }
}

@Composable
fun TrackMappingList() {

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioGraphList() {
    Row {
        Button(onClick = { model.processNewAudioGraph(false) }) {
            Text("New AudioGraph")
        }
        val cells = GridCells.Adaptive(0.dp)
        LazyVerticalGrid(cells) {
            val gridScope = this
            model.project.audioGraphs.forEach {
                gridScope.item {
                    Text(it.id ?: "")
                }
                gridScope.item {
                    Text(it.source ?: "")
                }
                gridScope.item {
                    Button(onClick = {}) {
                        Text("DEL")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MasterPluginList() {

    Row {

        Button(onClick = {}) {
            Text("New AudioGraph")
        }
        val cells = GridCells.Adaptive(0.dp)
        LazyVerticalGrid(cells) {
            val gridScope = this
            model.project.masterPlugins.forEach { id ->
                gridScope.item {
                    Text(id)
                }
                gridScope.item {
                    Button(onClick = {}) {
                        Text("DEL")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppSettings() {
    val cells = GridCells.Adaptive(2.dp)
    var augenePlayerPathState by remember { mutableStateOf(model.configAugenePlayerPath) }
    var audioPluginHostPathState by remember { mutableStateOf(model.configAudioPluginHostPath) }

    Column {
        Row {
            Text("Path to AugenePlayer")
            TextField(
                onValueChange = { augenePlayerPathState = it.text },
                value = TextFieldValue(augenePlayerPathState ?: "")
            )
            Button(onClick = {
                model.dialogs.ShowOpenFileDialog("Select AugenePlayer executable") {
                    if (it.any())
                        augenePlayerPathState = it.first()
                }
            }) {
                Text("Select")
            }
        }
        Row {
            Text("Path to AudioPluginHost")
            TextField(
                onValueChange = { audioPluginHostPathState = it.text },
                value = TextFieldValue(audioPluginHostPathState ?: "")
            )
            Button(onClick = {
                model.dialogs.ShowOpenFileDialog("Select AugenePlayer executable") {
                    if (it.any())
                        audioPluginHostPathState = it.first()
                }
            }) {
                Text("Select")
            }
        }
        Row {
            Button(onClick = {
                model.configAugenePlayerPath = augenePlayerPathState
                model.configAudioPluginHostPath = audioPluginHostPathState
                model.saveConfiguration()
            }) {
                Text("Apply")
            }
        }
        var autoReloadState by remember { mutableStateOf(false) }
        Row {
            Checkbox(checked = autoReloadState, onCheckedChange = {
                model.setAutoReloadProject(it)
                autoReloadState = it
            })
            Text("Auto reload project")
        }
        var autoRecompileState by remember { mutableStateOf(false) }
        Row {
            Checkbox(checked = autoRecompileState, onCheckedChange = {
                model.setAutoRecompileProject(it)
                autoRecompileState = it
            })
            Text("Auto compile project")
        }
    }
}