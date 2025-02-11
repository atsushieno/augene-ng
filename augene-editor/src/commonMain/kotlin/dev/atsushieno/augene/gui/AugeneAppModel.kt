package dev.atsushieno.augene.gui

import androidx.compose.runtime.mutableStateOf
import dev.atsushieno.augene.*
import dev.atsushieno.missingdot.xml.XmlNodeType
import dev.atsushieno.missingdot.xml.XmlReader
import dev.atsushieno.missingdot.xml.XmlTextWriter
import okio.Path.Companion.toPath

class AugeneAppModel : AugeneCompiler() {
	companion object {
		val instance = AugeneAppModel()

		const val ConfigXmlFile = "augene-config.xml"
	}

	fun processExit() {
		fileWatcher.terminate()
	}

	fun loadConfiguration () {
		val fs = IsolatedStorageFile.getUserStoreForAssembly("augene-ng")
		if (!fs.fileExists (ConfigXmlFile))
			return
		try {
			val fileContent = fs.readFileContentString(ConfigXmlFile)
			val xr = XmlReader.create(fileContent)
			xr.moveToContent ()
			if (xr.isEmptyElement) {
				xr.close()
				return
			}
			xr.readStartElement ("config")
			xr.moveToContent ()
			while (xr.nodeType == XmlNodeType.Element) {
				val name = xr.localName
				val s = xr.readElementContentAsString()
				when (name) {
					"AugenePlayer" -> configAugenePlayerPath = s
					"AudioPluginHost" -> configAudioPluginHostPath = s
					"LastProjectFile" -> lastProjectFile = s
					"AutoReloadProject" -> autoReloadProject.value = s.toBoolean()
					"AutoCompileProject" -> autoRecompileProject.value = s.toBoolean()
				}
				xr.moveToContent ()
			}
			xr.close()

			if (lastProjectFile != null)
				loadProjectFile(lastProjectFile!!)
		} catch (ex: Exception) {
			println (ex.toString())
			model.warningDialogMessage.value = "Failed to load configuration file. It is ignored."
		}
	}

	fun saveConfiguration () {
		val fs = IsolatedStorageFile.getUserStoreForAssembly("augene-ng")
		val sb = StringBuilder()
		val xw = XmlTextWriter(sb).apply { indent = true }
		xw.writeStartElement("config")
		xw.writeElementString("AugenePlayer", configAugenePlayerPath ?: "")
		xw.writeElementString("AudioPluginHost", configAudioPluginHostPath ?: "")
		xw.writeElementString("LastProjectFile", lastProjectFile ?: "")
		xw.writeElementString("AutoReloadProject", autoReloadProject.value.toString())
		xw.writeElementString("AutoCompileProject", autoRecompileProject.value.toString())
		xw.close()
		fs.writeFileContentString(ConfigXmlFile, sb.toString())
	}

	var refreshRequested : () -> Unit = {}

	fun processCreateNewProject () {
		dialogs.ShowSaveFileDialog ("Name a new Augene Project") { files ->
			if (files.any()) {
				projectFileName = files[0]
				project = AugeneProject()
				processSaveProject()
			}
		}
	}

	fun processOpenProject () {
		dialogs.ShowOpenFileDialog ("Open Augene Project") { files ->
			if (files.any()) {
				try {
					loadProjectFile(files[0])
				} catch (ex: Exception) {
					println(ex)
					warningDialogMessage.value = ex.message ?: ""
				}
			}
		}
	}

	fun processSaveProject () {
		if (projectFileName == null) {
			dialogs.ShowSaveFileDialog("Save Augene Project") { files ->
				if (files.any())
					projectFileName = files[0]
				else
					return@ShowSaveFileDialog
				AugeneProjectSaver.save(project, projectFileName!!)
			}
		}
		else
			AugeneProjectSaver.save(project, projectFileName!!)
	}

	fun processNewTrack (selectFileInsteadOfNewFile: Boolean) {
		if (selectFileInsteadOfNewFile) {
			dialogs.ShowOpenFileDialog ("Select existing AudioGraph file for a new track",
				DialogAbstraction.DialogOptions().apply { initialDirectory = projectDirectory }) { files ->
				if (files.any())
					addNewTrack(files[0])
			}
		} else {
			dialogs.ShowSaveFileDialog ("New AudioGraph file for a new track",
				DialogAbstraction.DialogOptions().apply { initialDirectory = projectDirectory }) { files ->
				if (files.any()) {
					FileSupport(projectFileName!!).writeString(files[0], JuceAudioGraph.emptyAudioGraph)
					addNewTrack(files[0])
				}
			}
		}
	}

	fun processNewAudioGraph (selectFileInsteadOfNewFile: Boolean) {
		if (selectFileInsteadOfNewFile) {
			dialogs.ShowOpenFileDialog ("Select existing AudioGraph file",
				DialogAbstraction.DialogOptions().apply { initialDirectory = projectDirectory }) { files ->
				if (files.any())
					addNewAudioGraph(files[0])
			}
		} else {
			dialogs.ShowSaveFileDialog ("New AudioGraph file",
				DialogAbstraction.DialogOptions().apply { initialDirectory = projectDirectory }) { files ->
				if (files.any()) {
					FileSupport(projectFileName!!).writeString(files[0], JuceAudioGraph.emptyAudioGraph)
					addNewAudioGraph(files[0])
				}
			}
		}
	}

	fun processDeleteTracks (trackIdsToRemove: Iterable<String>) {
		deleteTracks(trackIdsToRemove)
	}

	fun processDeleteAudioGraphs (audioGraphIdsToRemove: Iterable<String>) {
		deleteAudioGraphs(audioGraphIdsToRemove)
	}

	fun processNewMmlFile (selectFileInsteadOfNewFile: Boolean) {
		if (selectFileInsteadOfNewFile) {
			dialogs.ShowOpenFileDialog ("Select existing MML file",
				DialogAbstraction.DialogOptions().apply { initialDirectory = projectDirectory }) { files ->
				if (files.any())
					addNewMmlFile(files[0])
			}
		} else {
			dialogs.ShowSaveFileDialog ("New MML file",
				DialogAbstraction.DialogOptions().apply { initialDirectory = projectDirectory }) { files ->
				if (files.any()) {
					FileSupport(projectFileName!!).writeString(files[0], "// New MML file")
					addNewMmlFile(files[0])
				}
			}
		}
	}

	fun processUnregisterMmlFiles (filesToUnregister: Iterable<String>) {
		unregisterMmlFiles(filesToUnregister)
	}

	fun processLaunchAudioPluginHost (audioGraphFile: String) {
		if (configAudioPluginHostPath == null)
			model.warningDialogMessage.value = "AudioPluginHost path is not configured [File > Configure]."
		else {
			launchExternalProcess(configAudioPluginHostPath!!, getItemFileAbsolutePath (audioGraphFile))
		}
	}

	fun processNewMasterPluginFile (selectFileInsteadOfNewFile: Boolean) {
		if (selectFileInsteadOfNewFile) {
			dialogs.ShowOpenFileDialog ("Select existing AudioGraph file as a master plugin") { files ->
				if (files.any())
					addNewMasterPluginFile(files[0])
			}
		} else {
			dialogs.ShowSaveFileDialog ("New AudioGraph file as a master plugin") { files ->
				if (files.any()) {
					FileSupport(projectFileName!!).writeString(files[0], JuceAudioGraph.emptyAudioGraph)
					addNewMasterPluginFile(files[0])
				}
			}
		}
	}

	fun processUnregisterMasterPluginFiles (filesToUnregister: Iterable<String>) {
		unregisterMasterPluginFiles(filesToUnregister)
	}

	private fun onFileEvent(filePath: String, eventType: FileWatcher.EventType) {
		if (!autoReloadProject.value && !autoRecompileProject.value)
			return
		val proj: String = projectFileName ?: return
		if (filePath == projectFileName || project.mmlFiles.any { m -> (proj.toPath().parent!! / m).toString() == filePath }) {
			if (autoReloadProject.value)
				loadProjectFile(proj)
			if (autoRecompileProject.value)
				compile ()
		}
		// FIXME: otherwise, it is raised for content files. Reload and trigger recompilation.
	}

	private fun updateAutoReloadSetup () {
	}

	private val fileWatcherEventListener = object: FileSystemEventListener {
		override fun onEvent(filePath: String, eventType: FileWatcher.EventType) = onFileEvent(filePath, eventType)
	}

	private val fileWatcher = FileWatcher().apply {
		addChangeListener(fileWatcherEventListener)
	}

	fun processCompile () {
		if (projectFileName == null)
			processSaveProject ()
		if (projectFileName != null)
			try {
				compile()
			} catch (ex: Exception) {
				println(ex)
				model.warningDialogMessage.value = "Compilation error: ${ex.message}"
			}
	}

	fun processPlay () {
		if (configAugenePlayerPath.isNullOrEmpty())
			model.warningDialogMessage.value = "AugenePlayer path is not configured [File > Configure]."
		else {
			processCompile ()
			if (outputEditFileName != null)
				launchExternalProcess(configAugenePlayerPath!!, outputEditFileName!!)
		}
	}

	fun openFileOrContainingFolder (fullPath: String) = runFileOrFolderLauncher(fullPath)

	var autoReloadProject = mutableStateOf(false)

	var autoRecompileProject = mutableStateOf(false)

	var configAudioPluginHostPath: String? = null

	var configAugenePlayerPath: String? = null

	lateinit var dialogs: DialogAbstraction

	var lastLoadedProjectFileName : String? = null

	fun appOnProjectLoaded() {
		if (lastLoadedProjectFileName != projectFileName) {
			// FIXME: it is kind of hack, but so far we unify history with config.
			saveConfiguration()

			val path = projectFileName?.toPath()?.parent?.toString()
			if (path != null)
				fileWatcher.addTargetPath(path)
			if (!fileWatcher.enableRaisingEvents)
				fileWatcher.enableRaisingEvents = true

			updateAutoReloadSetup ()
		}

		lastLoadedProjectFileName = projectFileName

		refreshRequested()
	}

	init {
		onProjectLoaded = { appOnProjectLoaded() }
		onTrackAdded = { refreshRequested.invoke () }
		onTracksDeleted = { refreshRequested.invoke () }
		onAudioGraphAdded = { refreshRequested.invoke () }
		onAudioGraphsDeleted = { refreshRequested.invoke () }
		onMmlFileAdded = { refreshRequested.invoke () }
		onMmlFilesDeleted = { refreshRequested.invoke () }
		onMasterPluginAdded = { refreshRequested.invoke () }
		onMasterPluginsDeleted = { refreshRequested.invoke () }
	}

	var warningDialogMessage = mutableStateOf("")
}
