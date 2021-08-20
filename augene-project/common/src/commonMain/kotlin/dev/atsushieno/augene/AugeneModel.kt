package dev.atsushieno.augene

import dev.atsushieno.kotractive.*
import dev.atsushieno.ktmidi.MidiCC
import dev.atsushieno.midi2tracktionedit.MidiImportContext
import dev.atsushieno.midi2tracktionedit.MidiToTracktionEditConverter
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

		fun ToTracktion (src: Iterable<AugenePluginSpecifier>):  Iterable<PluginElement> {
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

	var AutoReloadProject: Boolean = false
	
	var AutoCompileProject : Boolean = false
	
	var Project = AugeneProject ()
	var ProjectFileName: String? = null

	var OutputEditFileName: String? = null

	var ConfigAudioPluginHostPath: String? = null

	var ConfigAugenePlayerPath: String? = null

	var LastProjectFile: String? = null

	lateinit var Dialogs: DialogAbstraction

	val ProjectDirectory : String
		get() = File(ProjectFileName!!).parent

	fun LoadConfiguration () {
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
					"AugenePlayer" -> ConfigAugenePlayerPath = s
					"AudioPluginHost" -> ConfigAudioPluginHostPath = s
					"LastProjectFile" -> LastProjectFile = s
				}
				xr.moveToContent ()
			}
			xr.close()
		} catch (ex: Exception) {
			println (ex.toString())
			Dialogs.ShowWarning ("Failed to load configuration file. It is ignored.") {}
		}
	}

	
	fun SaveConfiguration () {
		val fs = IsolatedStorageFile.getUserStoreForAssembly("augene-ng")
		val sb = StringBuilder()
			val xw = XmlWriter.create(sb)
			xw.writeStartElement("config")
			xw.writeElementString("AugenePlayer", ConfigAugenePlayerPath ?: "")
			xw.writeElementString("AudioPluginHost", ConfigAudioPluginHostPath ?: "")
			xw.writeElementString("LastProjectFile", LastProjectFile ?: "")
			xw.close()
		fs.writeFileContentString(ConfigXmlFile, sb.toString())
	}

	var RefreshRequested : () -> Unit = {}

	@OptIn(ExperimentalFileSystem::class)
	fun GetItemFileRelativePath (itemFilename: String) : String {
		var filenameRelative = itemFilename
		if (ProjectFileName != null)
			filenameRelative = (ProjectFileName!!.toPath() / itemFilename.toPath()).toString ()
		return filenameRelative
	}

	fun GetItemFileAbsolutePath (itemFilename: String) =
		File(ProjectFileName!!).parentFile.resolve(itemFilename).absolutePath

	fun ProcessOpenProject () {
		Dialogs.ShowOpenFileDialog ("Open Augene Project") { files ->
			if (files.any())
				ProcessLoadProjectFile(files[0])
		}
	}

	fun ProcessLoadProjectFile (file: String) {
		val prevFile = ProjectFileName
		Project = AugeneProject.load (file)
		ProjectFileName = file
		LastProjectFile = ProjectFileName
		if (prevFile != file) {
			// FIXME: it is kind of hack, but so far we unify history with config.
			SaveConfiguration()

			project_file_watcher.path = File(ProjectFileName!!).parent
			if (!project_file_watcher.enableRaisingEvents)
				project_file_watcher.enableRaisingEvents = true

			UpdateAutoReloadSetup ()
		}

		RefreshRequested()
	}

	fun ProcessSaveProject () {
		if (ProjectFileName == null) {
			Dialogs.ShowSaveFileDialog("Save Augene Project") { files ->
				if (files.any())
					ProjectFileName = files[0]
				else
					return@ShowSaveFileDialog
				AugeneProject.save (Project, ProjectFileName!!)
			}
		}
		else
			AugeneProject.save (Project, ProjectFileName!!)
	}

	@OptIn(ExperimentalFileSystem::class)
	fun ProcessNewTrack (selectFileInsteadOfNewFile: Boolean) {
		if (selectFileInsteadOfNewFile) {
			Dialogs.ShowOpenFileDialog ("Select existing AudioGraph file for a new track",
				DialogAbstraction.DialogOptions().apply { initialDirectory = ProjectDirectory }) { files ->
				if (files.any())
					AddNewTrack(files[0])
			}
		} else {
			Dialogs.ShowSaveFileDialog ("New AudioGraph file for a new track",
				DialogAbstraction.DialogOptions().apply { initialDirectory = ProjectDirectory }) { files ->
				if (files.any()) {
					FileSupport(ProjectFileName!!).writeString(files[0], JuceAudioGraph.emptyAudioGraph)
					AddNewTrack(files[0])
				}
			}
		}
	}

	fun AddNewTrack (filename: String) {
		var newTrackId = 1 + Project.tracks.size
		while (Project.tracks.any { t -> t.id == newTrackId.toString () })
			newTrackId++
		Project.tracks.add (AugeneTrack().apply {
			id = newTrackId.toString ()
			audioGraph = GetItemFileRelativePath (filename)
		})

		RefreshRequested.invoke ()
	}

	fun ProcessDeleteTracks ( trackIdsToRemove: Iterable<String>) {
		val tracksRemaining = Project.tracks.filter { t -> !trackIdsToRemove.contains (t.id) }
		Project.tracks.clear ()
		Project.tracks.addAll (tracksRemaining)

		RefreshRequested.invoke ()
	}

	@OptIn(ExperimentalFileSystem::class)
	fun ProcessNewAudioGraph (selectFileInsteadOfNewFile: Boolean) {
		if (selectFileInsteadOfNewFile) {
			Dialogs.ShowOpenFileDialog ("Select existing AudioGraph file",
				DialogAbstraction.DialogOptions().apply { initialDirectory = ProjectDirectory }) { files ->
				if (files.any())
					AddNewAudioGraph(files[0])
			}
		} else {
			Dialogs.ShowSaveFileDialog ("New AudioGraph file",
				DialogAbstraction.DialogOptions().apply { initialDirectory = ProjectDirectory }) { files ->
				if (files.any()) {
					FileSupport(ProjectFileName!!).writeString(files[0], JuceAudioGraph.emptyAudioGraph)
					AddNewAudioGraph(files[0])
				}
			}
		}
	}

	fun AddNewAudioGraph (filename: String) {
		var newGraphId = 1 + Project.audioGraphs.size
		while (Project.tracks.any { t -> t.id == newGraphId.toString () })
			newGraphId++
		Project.audioGraphs.add (AugeneAudioGraph().apply {
			id = newGraphId.toString ()
			source = GetItemFileRelativePath (filename)
		})

		RefreshRequested.invoke ()
	}

	fun ProcessDeleteAudioGraphs (audioGraphIdsToRemove: Iterable<String>) {
		val graphsRemaining = Project.audioGraphs.filter { t -> !audioGraphIdsToRemove.contains (t.id) }
		Project.audioGraphs.clear ()
		Project.audioGraphs.addAll (graphsRemaining)

		RefreshRequested.invoke ()
	}

	@OptIn(ExperimentalFileSystem::class)
	fun ProcessNewMmlFile (selectFileInsteadOfNewFile: Boolean) {
		if (selectFileInsteadOfNewFile) {
			Dialogs.ShowOpenFileDialog ("Select existing MML file",
				DialogAbstraction.DialogOptions().apply { initialDirectory = ProjectDirectory }) { files ->
				if (files.any())
					AddNewMmlFile(files[0])
			}
		} else {
			Dialogs.ShowSaveFileDialog ("New MML file",
				DialogAbstraction.DialogOptions().apply { initialDirectory = ProjectDirectory }) { files ->
				if (files.any()) {
					FileSupport(ProjectFileName!!).writeString(files[0], "// New MML file")
					AddNewMmlFile(files[0])
				}
			}
		}
	}
	fun AddNewMmlFile (filename: String) {
		Project.mmlFiles.add (GetItemFileRelativePath (filename))

		RefreshRequested.invoke ()
	}

	fun ProcessUnregisterMmlFiles (filesToUnregister: Iterable<String>) {
		val filesRemaining = Project.mmlFiles.filter { f -> !filesToUnregister.contains (f) }
		Project.mmlFiles.clear ()
		Project.mmlFiles.addAll (filesRemaining)

		RefreshRequested.invoke ()
	}

	fun ProcessLaunchAudioPluginHost (audioGraphFile: String) {
		if (ConfigAudioPluginHostPath == null)
			Dialogs.ShowWarning ("AudioPluginHost path is not configured [File > Configure].") {}
		else {
			ProcessBuilder(ConfigAudioPluginHostPath, GetItemFileAbsolutePath (audioGraphFile)).start()
		}
	}

	@OptIn(ExperimentalFileSystem::class)
	fun ProcessNewMasterPluginFile (selectFileInsteadOfNewFile: Boolean) {
		if (selectFileInsteadOfNewFile) {
			Dialogs.ShowOpenFileDialog ("Select existing AudioGraph file as a master plugin") { files ->
				if (files.any())
					AddNewMasterPluginFile(files[0])
			}
		} else {
			Dialogs.ShowSaveFileDialog ("New AudioGraph file as a master plugin") { files ->
				if (files.any()) {
					FileSupport(ProjectFileName!!).writeString(files[0], JuceAudioGraph.emptyAudioGraph)
					AddNewMasterPluginFile(files[0])
				}
			}
		}
	}
	fun AddNewMasterPluginFile (filename: String) {
		Project.masterPlugins.add (GetItemFileRelativePath (filename))

		RefreshRequested.invoke ()
	}

	fun ProcessUnregisterMasterPluginFiles (filesToUnregister: Iterable<String>) {
		val filesRemaining = Project.masterPlugins.filter { f -> !filesToUnregister.contains (f) }
		Project.masterPlugins.clear ()
		Project.masterPlugins.addAll (filesRemaining)

		RefreshRequested.invoke ()
	}

	fun SetAutoReloadProject(value: Boolean) {
		AutoReloadProject = value

		UpdateAutoReloadSetup ()
	}

	fun SetAutoRecompileProject(value: Boolean) {
		AutoCompileProject = value
	}

	private fun onFileEvent(o: Any, e: FileSystemWatcherEventArgs) {
		if (!AutoReloadProject && !AutoCompileProject)
			return
		val proj: String = ProjectFileName ?: return
		if (e.fullPath != ProjectFileName && Project.mmlFiles.all { m -> File(proj).parentFile.resolve(m).absolutePath != e.fullPath })
			return
		if (AutoReloadProject)
			ProcessLoadProjectFile (proj)

		if (AutoCompileProject)
			Compile ()
	}

	fun UpdateAutoReloadSetup () {
		project_file_watcher.addChangeListener { o, e -> onFileEvent(o, e) }
	}

	private val project_file_watcher = FileSystemWatcher()

	fun ProcessCompile () {
		if (ProjectFileName == null)
			ProcessSaveProject ()
		if (ProjectFileName != null)
			Compile ()
	}

	fun ReportError (errorId: String, msg: String) {
		// FIXME: appropriate error reporting
		println ("$errorId: $msg")
	}

	fun ResolvePathRelativetoProject (pathSpec: String) : String =
		File (ProjectFileName!!).absoluteFile.parentFile.resolve(pathSpec).absolutePath

	@OptIn(ExperimentalFileSystem::class)
	fun Compile () {
		if (ProjectFileName == null)
			throw IllegalStateException ("To compile the project, ProjectFileName must be specified in prior")

		val fileSupport = FileSupport(ProjectFileName!!)
		val abspath = { s:String? -> ResolvePathRelativetoProject(s!!) }
		val compiler = MmlCompiler.create()
		val mmlFilesAbs = Project.mmlFiles.map { f -> abspath (f) }
		val mmls = mmlFilesAbs.map { f -> MmlInputSource (f, fileSupport.readString(f)) } +
			Project.mmlStrings.map { s -> MmlInputSource ("(no file)", s) }
		val music = compiler.compile (false, mmls.toTypedArray())
		val edit = EditElement ()
		val converter = MidiToTracktionEditConverter (MidiImportContext (music, edit))
		converter.importMusic ()
		val dstTracks = edit.Tracks.filterIsInstance<TrackElement>()

		val audioGraphs = Project.expandedAudioGraphsFullPath (abspath, null, null).asIterable ().toList()

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
				a.program == program &&
				(a.bankMsb == msb || a.bankMsb == null && msb == "0") &&
				(a.bankLsb == lsb || a.bankLsb == null && lsb == "0") }
			if (ag != null) {
				val existingPlugins = track.Plugins
				track.Plugins.clear ()
				val text = fileSupport.readString(abspath (ag.source))
				val graph = JuceAudioGraph.load(XmlReader.create(text)).asIterable()
				for (p in ToTracktion(AugenePluginSpecifier.fromAudioGraph(graph)))
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
			val existingPlugins = track.Plugins
			track.Plugins.clear ()
			val ag = audioGraphs.firstOrNull { a -> a.id == track.Extension_InstrumentName }
			if (ag != null) {
				val text = fileSupport.readString(abspath (ag.source))
				val graph = JuceAudioGraph.load(XmlReader.create(text)).asIterable()
				for (p in ToTracktion(AugenePluginSpecifier.fromAudioGraph(graph)))
					track.Plugins.add(p)
			}
			// recover volume and level at the end.
			for (p in existingPlugins)
				track.Plugins.add (p)
		}

		// Step 3: assign audio graphs by TRACKNAME (if named). It will overwrite all above.
		for (track in Project.tracks) {
			val dstTrack = dstTracks.firstOrNull { t -> t.Id == track.id } ?: continue
			val existingPlugins = dstTrack.Plugins
			dstTrack.Plugins.clear ()
			if (track.audioGraph != null) {
				// track's AudioGraph may be either a ID reference or a filename.
				val ag = audioGraphs.firstOrNull { a -> a.id == track.audioGraph }
				val agFile = ag?.source ?: track.audioGraph
				if (!File (abspath (agFile)).exists()) {
					ReportError ("AugeneAudioGraphNotFound", "AudioGraph does not exist: " + abspath (agFile))
					continue
				}
				val text = fileSupport.readString(abspath (agFile))
				val graph = JuceAudioGraph.load (XmlReader.create (text)).asIterable()
				for (p in ToTracktion (AugenePluginSpecifier.fromAudioGraph (graph)))
				dstTrack.Plugins.add (p)
			}
			// recover volume and level at the end.
			for (p in existingPlugins)
				dstTrack.Plugins.add (p)
		}

		for (masterPlugin in Project.masterPlugins) {
			// AudioGraph may be either a ID reference or a filename.
			val ag = audioGraphs.firstOrNull { a -> a.id == masterPlugin }
			val agFile = ag?.source ?: masterPlugin
			if (!File (abspath (agFile)).exists()) {
				ReportError ("AugeneAudioGraphNotFound", "AudioGraph does not exist: " + abspath (agFile))
				continue
			}
			val text = fileSupport.readString(abspath (agFile))
			val graph = JuceAudioGraph.load (XmlReader.create (text)).asIterable()
			for ( p in ToTracktion (AugenePluginSpecifier.fromAudioGraph(graph)))
				edit.MasterPlugins.add (p)
		}

		val outfile = OutputEditFileName ?: abspath (File(ProjectDirectory).resolve( File(ProjectFileName!!).nameWithoutExtension + ".tracktionedit").path)
		val sb = StringBuilder()
		fileSupport.writeString(outfile, sb.toString())
		OutputEditFileName = outfile
	}

	fun ProcessPlay () {
		if (ConfigAugenePlayerPath.isNullOrEmpty())
			Dialogs.ShowWarning ("AugenePlayer path is not configured [File > Configure].") {}
		else {
			ProcessCompile ()
			if (OutputEditFileName != null)
				ProcessBuilder (ConfigAugenePlayerPath, OutputEditFileName).start()
		}
	}

	fun OpenFileOrContainingFolder (fullPath: String) {
		val os = System.getProperty("os.name")
		if (os.contains("windows"))
			ProcessBuilder("explorer", fullPath).start()
		if (os.contains("mac"))
			ProcessBuilder("open", fullPath).start()
		else
			ProcessBuilder("xdg-open", fullPath).start()
	}
}

