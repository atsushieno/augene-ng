package dev.atsushieno.augene
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

val model : AugeneAppModel
    get() = AugeneAppModel.instance

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
                Button(onClick = {
                    fabActionMenuState = false
                    model.processOpenProject()
                }) { Text("Load") }
                Button(onClick = {
                    fabActionMenuState = false
                    model.processSaveProject()
                }) { Text("Save") }
                Button(onClick = {
                    fabActionMenuState = false
                    model.processCompile()
                }) { Text("Compile") }
                Button(onClick = {
                    fabActionMenuState = false
                    model.processPlay()
                }) { Text("Play") }
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MmlList() {
    Column {
        Row {
            Button(onClick = { model.processNewMmlFile(false) }) {
                Text("New MML File")
            }
        }
        model.project.mmlFiles.forEach {
            Row {
                Text(it)
                Button(onClick = { model.openFileOrContainingFolder(it) }) { Text("Open") }
                Button(onClick = { model.processUnregisterMmlFiles(listOf(it)) }) { Text("Delete") }
            }
        }
        var mmlUpdateState by remember { mutableStateOf(0) } // hack
        model.project.mmlStrings.forEachIndexed { index, text ->
            Row {
                TextField(text, modifier = Modifier.weight(1.0f, true), onValueChange = {
                    if (mmlUpdateState < Int.MAX_VALUE)
                        model.project.mmlStrings[index] = text
                    mmlUpdateState++
                })
                Button(onClick = { model.processUnregisterMmlFiles(listOf(text)) }) { "DEL" }
            }
        }
    }
}

@Composable
fun TrackMappingList() {

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioGraphList() {
    Column {
        Button(onClick = { model.processNewAudioGraph(false) }) {
            Text("New AudioGraph")
        }
        model.project.audioGraphs.forEach {
            Row {
                Card { Text(it.id ?: "", fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp, 0.dp)) }
                Text(it.source ?: "")
                Button(onClick = { if (it.source?.isNotEmpty() == true) model.processLaunchAudioPluginHost(it.source!!) }) { Text("Open") }
                Button(onClick = { model.processDeleteAudioGraphs(listOf(it.id!!)) }) { Text("Delete") }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MasterPluginList() {
    Column {
        Button(onClick = {}) {
            Text("New AudioGraph")
        }
        model.project.masterPlugins.forEach {
            Row {
                Text(it)
                Button(onClick = { model.processLaunchAudioPluginHost(it) }) { Text("Open") }
                Button(onClick = { model.processUnregisterMasterPluginFiles(listOf(it)) }) { Text("Delete") }
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