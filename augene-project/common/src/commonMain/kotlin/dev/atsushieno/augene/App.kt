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
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext

val model = AugeneModel()

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
                Button(onClick = {}) { Text("Load") }
                Button(onClick = {}) { Text("Save") }
                Button(onClick = {}) { Text("Configure") }
                Button(onClick = {}) { Text("Compile") }
                Button(onClick = {}) { Text("Play") }
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
        Button(onClick = { model.ProcessNewAudioGraph(false) }) {
            Text("New AudioGraph")
        }
        val cells = GridCells.Adaptive(0.dp)
        LazyVerticalGrid(cells) {
            val gridScope = this
            model.Project.AudioGraphs.forEach {
                gridScope.item {
                    Text(it.Id ?: "")
                }
                gridScope.item {
                    Text(it.Source ?: "")
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
            model.Project.MasterPlugins.forEach { id ->
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
    var augenePlayerPathState by remember { mutableStateOf(model.ConfigAugenePlayerPath) }
    var audioPluginHostPathState by remember { mutableStateOf(model.ConfigAudioPluginHostPath) }

    Column {
        Row {
            Text("Path to AugenePlayer")
            TextField(
                onValueChange = { augenePlayerPathState = it.text },
                value = TextFieldValue(augenePlayerPathState ?: "")
            )
            Button(onClick = {}) { Text("Select") }
        }
        Row {
            Text("Path to AudioPluginHost")
            TextField(
                onValueChange = { audioPluginHostPathState = it.text },
                value = TextFieldValue(audioPluginHostPathState ?: "")
            )
            Button(onClick = {}) { Text("Select") }
        }
        Row {
            Button(onClick = {
                model.ConfigAugenePlayerPath = augenePlayerPathState
                model.ConfigAudioPluginHostPath = audioPluginHostPathState
                model.SaveConfiguration()
            }) {
                Text("Apply")
            }
        }
    }
}