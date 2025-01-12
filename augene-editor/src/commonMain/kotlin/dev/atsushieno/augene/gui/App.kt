package dev.atsushieno.augene.gui
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
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
        val warningDialogMessage by remember { model.warningDialogMessage }

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
                    model.processCreateNewProject()
                }) { Text("New") }
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

            NavigationBar {
                navigationItemLabels.forEachIndexed { index, l ->
                    NavigationBarItem(selected = tabIndex == index, onClick = { tabIndex = index}, label = { Text(l) }, icon = {})
                }
            }
        }

        if (warningDialogMessage.isNotEmpty()) {
            val closeDialog = { model.warningDialogMessage.value = "" }
            AlertDialog(onDismissRequest = closeDialog,
                confirmButton = { Button(onClick = closeDialog) { Text("OK") } },
                text = { Text(warningDialogMessage) }
            )
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
    Column {
        Row {
            Button(onClick = { model.processNewMmlFile(false) }) {
                Text("New MML File")
            }
            Button(onClick = { model.processNewMmlFile(true) }) {
                Text("Add existing MML file")
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
            Text("New AudioGraph file")
        }
        Button(onClick = { model.processNewAudioGraph(true) }) {
            Text("Add existing AudioGraph file")
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
        Button(onClick = { model.processNewMasterPluginFile(false) }) {
            Text("New AudioGraph file")
        }
        Button(onClick = { model.processNewMasterPluginFile(true) }) {
            Text("Add existing AudioGraph file")
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
    var augenePlayerPathState by remember { mutableStateOf(model.configAugenePlayerPath) }
    var audioPluginHostPathState by remember { mutableStateOf(model.configAudioPluginHostPath) }
    var autoReloadState by remember { model.autoReloadProject }
    var autoRecompileState by remember { model.autoRecompileProject }

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
                model.autoReloadProject.value = autoReloadState
                model.autoRecompileProject.value = autoRecompileState
                model.saveConfiguration()
            }) {
                Text("Apply")
            }
        }
        Row {
            Checkbox(checked = autoReloadState, onCheckedChange = {
                autoReloadState = it
            })
            Text("Auto reload project")
        }
        Row {
            Checkbox(checked = autoRecompileState, onCheckedChange = {
                autoRecompileState = it
            })
            Text("Auto compile project")
        }
    }
}