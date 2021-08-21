package dev.atsushieno.augene

import dev.atsushieno.kotractive.*
import dev.atsushieno.ktmidi.MidiCC
import dev.atsushieno.augene.MidiImportContext
import dev.atsushieno.augene.MidiToTracktionEditConverter
import dev.atsushieno.mugene.MmlCompiler
import dev.atsushieno.mugene.MmlInputSource
import okio.ExperimentalFileSystem
import okio.Path.Companion.toPath
import java.io.File

class AugeneModel
{
	companion object {
		val instance = AugeneModel()

		const val ConfigXmlFile = "augene-config.xml"

		private const val TracktionProgramChange = 4097

		fun toTracktion (src: Iterable<AugenePluginSpecifier>):  Iterable<PluginElement> {
			return src.map { a ->
				PluginElement().apply {
					Filename = a.filename
					Enabled = true
					Uid = a.uid
					Type = a.type ?: "vst"
					Name = a.name
					Manufacturer = a.manufacturer
					State = a.state
					Volume = 1.0 // maybe? at least we have to avoid default 0.0
				}
			}
		}
	}

	private var _autoReloadProject: Boolean = false

	private var _autoCompileProject : Boolean = false
	
	var project = AugeneProject ()
	var projectFileName: String? = null

	var outputEditFileName: String? = null

	var configAudioPluginHostPath: String? = null

	var configAugenePlayerPath: String? = null

	var lastProjectFile: String? = null

	lateinit var dialogs: DialogAbstraction

	@OptIn(ExperimentalFileSystem::class)
	val projectDirectory : String?
		get() = if (projectFileName == null) null else projectFileName!!.toPath().parent.toString()

	fun loadConfiguration () {
		val fs = IsolatedStorageFile.getUserStoreForAssembly("augene-ng")
		if (!fs.fileExists (ConfigXmlFile))
			return
		try {
			val fileContent = fs.readFileContentString(ConfigXmlFile)
			val xr = XmlReader.create (fileContent)
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

	@OptIn(ExperimentalFileSystem::class)
	fun getItemFileRelativePath (itemFilename: String) : String {
		var filenameRelative = itemFilename
		if (projectFileName != null)
			filenameRelative = (projectFileName!!.toPath() / itemFilename.toPath()).toString ()
		return filenameRelative
	}

	@OptIn(ExperimentalFileSystem::class)
	fun getItemFileAbsolutePath (itemFilename: String) =
		(projectFileName!!.toPath().parent!! / itemFilename.toPath()).toString()

	fun processOpenProject () {
		dialogs.ShowOpenFileDialog ("Open Augene Project") { files ->
			if (files.any())
				processLoadProjectFile(files[0])
		}
	}

	fun processLoadProjectFile (file: String) {
		val prevFile = projectFileName
		project = AugeneProject.load (file)
		projectFileName = file
		lastProjectFile = projectFileName
		if (prevFile != file) {
			// FIXME: it is kind of hack, but so far we unify history with config.
			saveConfiguration()

			project_file_watcher.path = File(projectFileName!!).parent
			if (!project_file_watcher.enableRaisingEvents)
				project_file_watcher.enableRaisingEvents = true

			updateAutoReloadSetup ()
		}

		refreshRequested()
	}

	fun processSaveProject () {
		if (projectFileName == null) {
			dialogs.ShowSaveFileDialog("Save Augene Project") { files ->
				if (files.any())
					projectFileName = files[0]
				else
					return@ShowSaveFileDialog
				AugeneProject.save (project, projectFileName!!)
			}
		}
		else
			AugeneProject.save (project, projectFileName!!)
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

	fun addNewTrack (filename: String) {
		var newTrackId = 1 + project.tracks.size
		while (project.tracks.any { t -> t.id == newTrackId.toString () })
			newTrackId++
		project.tracks.add (AugeneTrack().apply {
			id = newTrackId.toString ()
			audioGraph = getItemFileRelativePath (filename)
		})

		refreshRequested.invoke ()
	}

	fun processDeleteTracks (trackIdsToRemove: Iterable<String>) {
		val tracksRemaining = project.tracks.filter { t -> !trackIdsToRemove.contains (t.id) }
		project.tracks.clear ()
		project.tracks.addAll (tracksRemaining)

		refreshRequested.invoke ()
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

	fun addNewAudioGraph (filename: String) {
		var newGraphId = 1 + project.audioGraphs.size
		while (project.tracks.any { t -> t.id == newGraphId.toString () })
			newGraphId++
		project.audioGraphs.add (AugeneAudioGraph().apply {
			id = newGraphId.toString ()
			source = getItemFileRelativePath (filename)
		})

		refreshRequested.invoke ()
	}

	fun processDeleteAudioGraphs (audioGraphIdsToRemove: Iterable<String>) {
		val graphsRemaining = project.audioGraphs.filter { t -> !audioGraphIdsToRemove.contains (t.id) }
		project.audioGraphs.clear ()
		project.audioGraphs.addAll (graphsRemaining)

		refreshRequested.invoke ()
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
	fun addNewMmlFile (filename: String) {
		project.mmlFiles.add (getItemFileRelativePath (filename))

		refreshRequested.invoke ()
	}

	fun processUnregisterMmlFiles (filesToUnregister: Iterable<String>) {
		val filesRemaining = project.mmlFiles.filter { f -> !filesToUnregister.contains (f) }
		project.mmlFiles.clear ()
		project.mmlFiles.addAll (filesRemaining)

		refreshRequested.invoke ()
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
	fun addNewMasterPluginFile (filename: String) {
		project.masterPlugins.add (getItemFileRelativePath (filename))

		refreshRequested.invoke ()
	}

	fun processUnregisterMasterPluginFiles (filesToUnregister: Iterable<String>) {
		val filesRemaining = project.masterPlugins.filter { f -> !filesToUnregister.contains (f) }
		project.masterPlugins.clear ()
		project.masterPlugins.addAll (filesRemaining)

		refreshRequested.invoke ()
	}

	fun setAutoReloadProject(value: Boolean) {
		_autoReloadProject = value

		updateAutoReloadSetup ()
	}

	fun setAutoRecompileProject(value: Boolean) {
		_autoCompileProject = value
	}

	private fun onFileEvent(o: Any, e: FileSystemWatcherEventArgs) {
		if (!_autoReloadProject && !_autoCompileProject)
			return
		val proj: String = projectFileName ?: return
		if (e.fullPath != projectFileName && project.mmlFiles.all { m -> File(proj).parentFile.resolve(m).absolutePath != e.fullPath })
			return
		if (_autoReloadProject)
			processLoadProjectFile (proj)

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

	fun reportError (errorId: String, msg: String) {
		// FIXME: appropriate error reporting
		println ("$errorId: $msg")
	}

	fun resolvePathRelativetoProject (pathSpec: String) : String =
		File (projectFileName!!).absoluteFile.parentFile.resolve(pathSpec).absolutePath

	@OptIn(ExperimentalFileSystem::class)
	fun compile () {
		if (projectFileName == null)
			throw IllegalStateException ("To compile the project, ProjectFileName must be specified in prior")

		val fileSupport = FileSupport(projectFileName!!)
		val abspath = { s:String? -> resolvePathRelativetoProject(s!!) }
		val compiler = MmlCompiler.create()
		val mmlFilesAbs = project.mmlFiles.map { f -> abspath (f) }
		val mmls = mmlFilesAbs.map { f -> MmlInputSource (f, fileSupport.readString(f)) } +
			project.mmlStrings.map { s -> MmlInputSource ("(no file)", s) }
		val music = compiler.compile (false, mmls.toTypedArray())
		val edit = EditElement ()
		val converter = MidiToTracktionEditConverter (MidiImportContext (music, edit))
		converter.importMusic ()
		val dstTracks = edit.Tracks.filterIsInstance<TrackElement>()

		val audioGraphs = project.expandedAudioGraphsFullPath (abspath, null, null).asIterable ().toList()

		// Assign numeric IDs to those unnamed tracks.
		for (n in dstTracks.indices)
			if (edit.Tracks [n].Id == null)
				edit.Tracks [n].Id = (n + 1).toString ()

		// Step 1: assign audio graphs by Bank Select and Program Change, if any.
		// Such a track must not contain more than one program change, bank select MSB and bank select LSB.
		for (track in edit.Tracks.filterIsInstance<TrackElement> ()) {
			val sequenceContainer = track.Clips.filterIsInstance<MidiClipElement> ()
				.filter { c -> c.Sequence != null }.toList()
			val programs = sequenceContainer.flatMap { c -> c.Sequence!!.Events.filterIsInstance<ControlElement> ()
					.filter { e -> e.Type == TracktionProgramChange } }
			val banks = sequenceContainer.flatMap { c -> c.Sequence!!.Events.filterIsInstance<ControlElement> ()
					.filter { e -> e.Type == MidiCC.BANK_SELECT } }
			val bankLSBs = sequenceContainer.flatMap { c -> c.Sequence!!.Events.filterIsInstance<ControlElement> ()
					.filter { e -> e.Type == MidiCC.BANK_SELECT_LSB } }
			if (programs.size != 1 || banks.size > 1 || bankLSBs.size > 1)
				continue // ignore
			val msb = ((banks.firstOrNull ()?.Val ?: 0) / 128).toString ()
			val lsb = ((bankLSBs.firstOrNull ()?.Val ?: 0) / 128).toString ()
			val program = (programs.first ().Val / 128).toString ()
			val ag = audioGraphs.firstOrNull { a ->
				a.retrieveProgram() == program &&
				(a.retrieveBankMsb() == msb || a.retrieveBankMsb() == null && msb == "0") &&
				(a.retrieveBankLsb() == lsb || a.retrieveBankLsb() == null && lsb == "0") }
			if (ag != null) {
				val existingPlugins = track.Plugins.toTypedArray()
				track.Plugins.clear ()
				val text = fileSupport.readString(abspath (ag.source))
				val graph = JuceAudioGraph.load(XmlReader.create(text)).asIterable()
				for (p in toTracktion(AugenePluginSpecifier.fromAudioGraph(graph)))
					track.Plugins.add(p)
				// recover volume and level at the end.
				for (p in existingPlugins)
					track.Plugins.add (p)
			}
		}

		// Step 2: assign audio graphs by INSTRUMENTNAME (if named). It will overwrite bank mapping.
		for (track in edit.Tracks.filterIsInstance<TrackElement> ()) {
			if (track.Extension_InstrumentName == null)
				continue
			val existingPlugins = track.Plugins.toTypedArray()
			track.Plugins.clear ()
			val ag = audioGraphs.firstOrNull { a -> a.id == track.Extension_InstrumentName }
			if (ag != null) {
				val text = fileSupport.readString(abspath (ag.source))
				val graph = JuceAudioGraph.load(XmlReader.create(text)).asIterable()
				for (p in toTracktion(AugenePluginSpecifier.fromAudioGraph(graph)))
					track.Plugins.add(p)
			}
			// recover volume and level at the end.
			for (p in existingPlugins)
				track.Plugins.add (p)
		}

		// Step 3: assign audio graphs by TRACKNAME (if named). It will overwrite all above.
		for (track in project.tracks) {
			val dstTrack = dstTracks.firstOrNull { t -> t.Id == track.id } ?: continue
			val existingPlugins = dstTrack.Plugins.toTypedArray()
			dstTrack.Plugins.clear ()
			if (track.audioGraph != null) {
				// track's AudioGraph may be either a ID reference or a filename.
				val ag = audioGraphs.firstOrNull { a -> a.id == track.audioGraph }
				val agFile = ag?.source ?: track.audioGraph
				if (!File (abspath (agFile)).exists()) {
					reportError ("AugeneAudioGraphNotFound", "AudioGraph does not exist: " + abspath (agFile))
					continue
				}
				val text = fileSupport.readString(abspath (agFile))
				val graph = JuceAudioGraph.load (XmlReader.create (text)).asIterable()
				for (p in toTracktion (AugenePluginSpecifier.fromAudioGraph (graph)))
				dstTrack.Plugins.add (p)
			}
			// recover volume and level at the end.
			for (p in existingPlugins)
				dstTrack.Plugins.add (p)
		}

		for (masterPlugin in project.masterPlugins) {
			// AudioGraph may be either a ID reference or a filename.
			val ag = audioGraphs.firstOrNull { a -> a.id == masterPlugin }
			val agFile = ag?.source ?: masterPlugin
			if (!File (abspath (agFile)).exists()) {
				reportError ("AugeneAudioGraphNotFound", "AudioGraph does not exist: " + abspath (agFile))
				continue
			}
			val text = fileSupport.readString(abspath (agFile))
			val graph = JuceAudioGraph.load (XmlReader.create (text)).asIterable()
			for ( p in toTracktion (AugenePluginSpecifier.fromAudioGraph(graph)))
				edit.MasterPlugins.add (p)
		}

		val outfile = outputEditFileName ?: abspath (File(projectDirectory!!).resolve( File(projectFileName!!).nameWithoutExtension + ".tracktionedit").path)
		val sb = StringBuilder()
		EditModelWriter().write(sb, edit)
		fileSupport.writeString(outfile, sb.toString())
		outputEditFileName = outfile
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
}

