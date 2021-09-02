package dev.atsushieno.augene

import dev.atsushieno.kotractive.*
import dev.atsushieno.ktmidi.MidiCC
import dev.atsushieno.missingdot.xml.XmlReader
import dev.atsushieno.missingdot.xml.XmlTextReader
import dev.atsushieno.mugene.MmlCompiler
import dev.atsushieno.mugene.MmlInputSource
import okio.ExperimentalFileSystem
import okio.Path.Companion.toPath

open class AugeneModel
{
	companion object {
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

	var project = AugeneProject ()
	var projectFileName: String? = null

	var outputEditFileName: String? = null

	var lastProjectFile: String? = null

	@OptIn(ExperimentalFileSystem::class)
	val projectDirectory : String?
		get() = if (projectFileName == null) null else projectFileName!!.toPath().parent.toString()

	open fun reportError (errorId: String, msg: String) {
		// FIXME: appropriate error reporting
		println ("$errorId: $msg")
	}

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

	fun loadProjectFile (xmlFile: String) =
		setProject(AugeneProject.load(xmlFile), xmlFile)

	fun loadProjectJson (text: String, baseFileName: String = ".") =
		setProject(AugeneProject.loadJson (text), baseFileName)

	fun setProject(newProject: AugeneProject, baseFileName: String) {
		project = newProject
		projectFileName = baseFileName
		lastProjectFile = projectFileName
		onProjectLoaded()
	}
	var onProjectLoaded : () -> Unit = {}

	fun addNewTrack (filename: String) {
		var newTrackId = 1 + project.tracks.size
		while (project.tracks.any { t -> t.id == newTrackId.toString () })
			newTrackId++
		project.tracks.add (AugeneTrack().apply {
			id = newTrackId.toString ()
			audioGraph = getItemFileRelativePath (filename)
		})

		onTrackAdded()
	}
	var onTrackAdded : () -> Unit = {}

	fun deleteTracks (trackIdsToRemove: Iterable<String>) {
		val tracksRemaining = project.tracks.filter { t -> !trackIdsToRemove.contains (t.id) }
		project.tracks.clear ()
		project.tracks.addAll (tracksRemaining)

		onTracksDeleted()
	}
	var onTracksDeleted : () -> Unit = {}

	fun addNewAudioGraph (filename: String) {
		var newGraphId = 1 + project.audioGraphs.size
		while (project.tracks.any { t -> t.id == newGraphId.toString () })
			newGraphId++
		project.audioGraphs.add (AugeneAudioGraph().apply {
			id = newGraphId.toString ()
			source = getItemFileRelativePath (filename)
		})

		onAudioGraphAdded()
	}
	var onAudioGraphAdded : () -> Unit = {}

	fun deleteAudioGraphs (audioGraphIdsToRemove: Iterable<String>) {
		val graphsRemaining = project.audioGraphs.filter { t -> !audioGraphIdsToRemove.contains (t.id) }
		project.audioGraphs.clear ()
		project.audioGraphs.addAll (graphsRemaining)

		onAudioGraphsDeleted()
	}
	var onAudioGraphsDeleted : () -> Unit = {}

	fun addNewMmlFile (filename: String) {
		project.mmlFiles.add (getItemFileRelativePath (filename))

		onMmlFileAdded()
	}
	var onMmlFileAdded : () -> Unit = {}

	fun unregisterMmlFiles (filesToUnregister: Iterable<String>) {
		val filesRemaining = project.mmlFiles.filter { f -> !filesToUnregister.contains (f) }
		project.mmlFiles.clear ()
		project.mmlFiles.addAll (filesRemaining)

		onMmlFilesDeleted()
	}
	var onMmlFilesDeleted : () -> Unit = {}

	fun addNewMasterPluginFile (filename: String) {
		project.masterPlugins.add (getItemFileRelativePath (filename))

		onMasterPluginAdded()
	}
	var onMasterPluginAdded : () -> Unit = {}

	fun unregisterMasterPluginFiles (filesToUnregister: Iterable<String>) {
		val filesRemaining = project.masterPlugins.filter { f -> !filesToUnregister.contains (f) }
		project.masterPlugins.clear ()
		project.masterPlugins.addAll (filesRemaining)

		onMasterPluginsDeleted()
	}
	var onMasterPluginsDeleted : () -> Unit = {}

	@OptIn(ExperimentalFileSystem::class)
	fun compile () {
		if (projectFileName == null)
			throw IllegalStateException ("To compile the project, ProjectFileName must be specified in prior")

		val fileSupport = FileSupport(projectFileName!!)
		val abspath = { s:String? -> fileSupport.resolvePathRelativeToProject(s!!) }

		// compile into MidiMusic.
		val compiler = MmlCompiler.create()
		val mmlFilesAbs = project.mmlFiles.map { f -> abspath (f) }
		val mmls = mmlFilesAbs.map { f -> MmlInputSource (f, fileSupport.readString(f)) } +
			project.mmlStrings.map { s -> MmlInputSource ("(no file)", s) }
		val music = compiler.compile (false, mmls.toTypedArray())

		// load filtergraphs here so that they can be referenced at importing.
		val audioGraphs = project.expandedAudioGraphsFullPath (abspath, null, null).asIterable ().toMutableList()
		val juceAudioGraphs = audioGraphs.filter { it.source != null && it.id != null }.map {
			val text = fileSupport.readString(abspath(it.source))
			it.id!! to JuceAudioGraph.load(XmlReader.create(text)).asIterable()
		}.toMap()

		// prepare tracktionedit
		val edit = EditElement ()
		val converter = MidiToTracktionEditConverter (MidiImportContext (music, edit, audioGraphs, juceAudioGraphs))
		converter.importMusic ()
		val dstTracks = edit.Tracks.filterIsInstance<TrackElement>()

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

		// there used to be Step 2: assign audio graphs by INSTRUMENTNAME (if named). It will overwrite bank mapping.
		// (Now it is done at Midi2TracktionEditConverter.importMusic() as PLUGIN elements should be already populated there.)

		// Step 3: assign audio graphs by TRACKNAME (if named). It will overwrite all above.
		for (track in project.tracks) {
			val dstTrack = dstTracks.firstOrNull { t -> t.Id == track.id } ?: continue
			val existingPlugins = dstTrack.Plugins.toTypedArray()
			dstTrack.Plugins.clear ()
			if (track.audioGraph != null) {
				// track's AudioGraph may be either a ID reference or a filename.
				val ag = audioGraphs.firstOrNull { a -> a.id == track.audioGraph }
				val agFile = ag?.source ?: track.audioGraph
				/* FIXME: maybe enable this proactive file check?
				if (!File (abspath (agFile)).exists()) {
					reportError ("AugeneAudioGraphNotFound", "AudioGraph does not exist: " + abspath (agFile))
					continue
				}*/
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
			// AudioGraph may be either an ID reference or a filename.
			val ag = audioGraphs.firstOrNull { a -> a.id == masterPlugin }
			val agFile = ag?.source ?: masterPlugin
			/* FIXME: maybe enable this proactive file check?
			if (!File (abspath (agFile)).exists()) {
				reportError ("AugeneAudioGraphNotFound", "AudioGraph does not exist: " + abspath (agFile))
				continue
			}
			*/
			val text = fileSupport.readString(abspath (agFile))
			val graph = JuceAudioGraph.load (XmlReader.create (text)).asIterable()
			for ( p in toTracktion (AugenePluginSpecifier.fromAudioGraph(graph)))
				edit.MasterPlugins.add (p)
		}

		fun changeExtension(path: String, ext: String) : String {
			val lastIndex = path.lastIndexOf('.')
			return if (lastIndex > 0) path.substring(0, lastIndex) + ext else path
		}

		val outfile = outputEditFileName ?: changeExtension(abspath (projectFileName!!), ".tracktionedit")
		val sb = StringBuilder()
		EditModelWriter().write(sb, edit)
		fileSupport.writeString(outfile, sb.toString())
		outputEditFileName = outfile
	}
}
