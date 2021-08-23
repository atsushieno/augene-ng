package dev.atsushieno.augene

import dev.atsushieno.missingdot.xml.XmlNodeType
import dev.atsushieno.missingdot.xml.XmlReader
import dev.atsushieno.missingdot.xml.XmlWriter
import okio.ExperimentalFileSystem
import okio.Path.Companion.toPath

class AugeneAppModel : AugeneModel() {
	companion object {
		val instance = AugeneAppModel()

		const val ConfigXmlFile = "augene-config.xml"
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
				}
				xr.moveToContent ()
			}
			xr.close()

			if (lastProjectFile != null)
				loadProjectFile(lastProjectFile!!)
		} catch (ex: Exception) {
			println (ex.toString())
			dialogs.ShowWarning ("Failed to load configuration file. It is ignored.") {}
		}
	}

	fun saveConfiguration () {
		val fs = IsolatedStorageFile.getUserStoreForAssembly("augene-ng")
		val sb = StringBuilder()
		val xw = XmlWriter.create(sb)
		xw.writeStartElement("config")
		xw.writeElementString("AugenePlayer", configAugenePlayerPath ?: "")
		xw.writeElementString("AudioPluginHost", configAudioPluginHostPath ?: "")
		xw.writeElementString("LastProjectFile", lastProjectFile ?: "")
		xw.close()
		fs.writeFileContentString(ConfigXmlFile, sb.toString())
	}

	var refreshRequested : () -> Unit = {}

	fun processOpenProject () {
		dialogs.ShowOpenFileDialog ("Open Augene Project") { files ->
			if (files.any())
				loadProjectFile(files[0])
		}
	}

	fun processSaveProject () {
		if (projectFileName == null) {
			dialogs.ShowSaveFileDialog("Save Augene Project") { files ->
				if (files.any())
					projectFileName = files[0]
				else
					return@ShowSaveFileDialog
                AugeneProject.save(project, projectFileName!!)
			}
		}
		else
            AugeneProject.save(project, projectFileName!!)
	}

	@OptIn(ExperimentalFileSystem::class)
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

	@OptIn(ExperimentalFileSystem::class)
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

	@OptIn(ExperimentalFileSystem::class)
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
			dialogs.ShowWarning ("AudioPluginHost path is not configured [File > Configure].") {}
		else {
			ProcessBuilder(configAudioPluginHostPath, getItemFileAbsolutePath (audioGraphFile)).start()
		}
	}

	@OptIn(ExperimentalFileSystem::class)
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

	fun setAutoReloadProject(value: Boolean) {
		_autoReloadProject = value

		updateAutoReloadSetup ()
	}

	fun setAutoRecompileProject(value: Boolean) {
		_autoCompileProject = value
	}

	@OptIn(ExperimentalFileSystem::class)
	private fun onFileEvent(o: Any, e: FileSystemWatcherEventArgs) {
		if (!_autoReloadProject && !_autoCompileProject)
			return
		val proj: String = projectFileName ?: return
		if (e.fullPath != projectFileName && project.mmlFiles.all { m -> (proj.toPath().parent!! / m).toString() != e.fullPath })
			return
		if (_autoReloadProject)
			loadProjectFile (proj)

		if (_autoCompileProject)
			compile ()
	}

	private fun updateAutoReloadSetup () {
		project_file_watcher.addChangeListener { o, e -> onFileEvent(o, e) }
	}

	private val project_file_watcher = FileSystemWatcher()

	fun processCompile () {
		if (projectFileName == null)
			processSaveProject ()
		if (projectFileName != null)
			compile ()
	}

	fun processPlay () {
		if (configAugenePlayerPath.isNullOrEmpty())
			dialogs.ShowWarning ("AugenePlayer path is not configured [File > Configure].") {}
		else {
			processCompile ()
			if (outputEditFileName != null)
				ProcessBuilder (configAugenePlayerPath, outputEditFileName).start()
		}
	}

	fun openFileOrContainingFolder (fullPath: String) {
		val os = System.getProperty("os.name")
		if (os.contains("windows"))
			ProcessBuilder("explorer", fullPath).start()
		if (os.contains("mac"))
			ProcessBuilder("open", fullPath).start()
		else
			ProcessBuilder("xdg-open", fullPath).start()
	}

	private var _autoReloadProject: Boolean = false

	private var _autoCompileProject : Boolean = false

	var configAudioPluginHostPath: String? = null

	var configAugenePlayerPath: String? = null

	lateinit var dialogs: DialogAbstraction

	var lastLoadedProjectFileName : String? = null

	@OptIn(ExperimentalFileSystem::class)
	fun appOnProjectLoaded() {
		if (lastLoadedProjectFileName != projectFileName) {
			// FIXME: it is kind of hack, but so far we unify history with config.
			saveConfiguration()

			project_file_watcher.path = projectFileName?.toPath()?.parent?.toString()
			if (!project_file_watcher.enableRaisingEvents)
				project_file_watcher.enableRaisingEvents = true

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
}
